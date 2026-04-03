package com.ywacko.aliyun.tdx.attestation.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ywacko.aliyun.tdx.attestation.exception.QuoteGenerationException;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ProcessQuoteProvider implements QuoteProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<String> helperCommand;
    private final Duration timeout;
    private final Map<String, String> environment;
    private final Path workingDirectory;

    private ProcessQuoteProvider(Builder builder) {
        if (builder.helperCommand.isEmpty()) {
            throw new IllegalArgumentException("helperCommand must not be empty");
        }
        this.helperCommand = List.copyOf(builder.helperCommand);
        this.timeout = builder.timeout;
        this.environment = Map.copyOf(builder.environment);
        this.workingDirectory = builder.workingDirectory;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public QuoteGenerationResult generateQuote(QuoteGenerationRequest request) {
        Objects.requireNonNull(request, "request");
        HelperQuoteRequest helperRequest = new HelperQuoteRequest(
                request.getReportData().toHex(),
                request.getDeploymentDigestHex()
        );

        ProcessBuilder processBuilder = new ProcessBuilder(helperCommand);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }
        processBuilder.environment().putAll(environment);

        try {
            Process process = processBuilder.start();
            byte[] requestBytes = OBJECT_MAPPER.writeValueAsBytes(helperRequest);
            process.getOutputStream().write(requestBytes);
            process.getOutputStream().close();

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new QuoteGenerationException("helper timed out after " + timeout);
            }

            byte[] stdout = process.getInputStream().readAllBytes();
            byte[] stderr = process.getErrorStream().readAllBytes();

            if (process.exitValue() != 0) {
                throw new QuoteGenerationException("helper exited with code " + process.exitValue()
                        + ", stderr=" + new String(stderr, StandardCharsets.UTF_8));
            }

            HelperQuoteResponse response = OBJECT_MAPPER.readValue(stdout, HelperQuoteResponse.class);
            if (response.getQuoteBase64() == null || response.getQuoteBase64().trim().isEmpty()) {
                throw new QuoteGenerationException("helper response missing quoteBase64");
            }

            byte[] quoteBytes = Base64.getDecoder().decode(response.getQuoteBase64());
            String reportDataHex = response.getReportDataHex() != null
                    ? response.getReportDataHex()
                    : request.getReportData().toHex();

            return new QuoteGenerationResult(
                    quoteBytes,
                    request.getDeploymentDigestHex(),
                    reportDataHex,
                    response.getQuoteSize(),
                    response.getProvider(),
                    response.getHelperVersion()
            );
        } catch (IOException e) {
            throw new QuoteGenerationException("failed to execute helper command " + helperCommand, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QuoteGenerationException("helper execution interrupted", e);
        }
    }

    public static final class Builder {
        private final List<String> helperCommand = new ArrayList<>();
        private Duration timeout = Duration.ofSeconds(15);
        private final Map<String, String> environment = new HashMap<>();
        private Path workingDirectory;

        private Builder() {
        }

        public Builder helperCommand(List<String> helperCommand) {
            this.helperCommand.clear();
            this.helperCommand.addAll(helperCommand);
            return this;
        }

        public Builder helperCommand(String... helperCommand) {
            this.helperCommand.clear();
            this.helperCommand.addAll(List.of(helperCommand));
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder environment(String key, String value) {
            this.environment.put(key, value);
            return this;
        }

        public Builder workingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public ProcessQuoteProvider build() {
            return new ProcessQuoteProvider(this);
        }
    }
}
