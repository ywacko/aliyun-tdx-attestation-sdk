package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import com.ywacko.aliyun.tdx.attestation.process.ProcessQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.process.QuoteProvider;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class AliyunTdxAttestationClient {

    private final QuoteProvider quoteProvider;

    private AliyunTdxAttestationClient(Builder builder) {
        this.quoteProvider = Objects.requireNonNull(builder.quoteProvider, "quoteProvider");
    }

    public static Builder builder() {
        return new Builder();
    }

    public QuoteGenerationResult generateQuote(DeploymentFingerprint fingerprint) {
        return generateQuote(QuoteGenerationRequest.fromDeploymentFingerprint(fingerprint));
    }

    public QuoteGenerationResult generateQuote(QuoteGenerationRequest request) {
        return quoteProvider.generateQuote(request);
    }

    public static final class Builder {
        private QuoteProvider quoteProvider;

        private Builder() {
        }

        public Builder quoteProvider(QuoteProvider quoteProvider) {
            this.quoteProvider = quoteProvider;
            return this;
        }

        public Builder helperCommand(List<String> helperCommand) {
            this.quoteProvider = ProcessQuoteProvider.builder()
                    .helperCommand(helperCommand)
                    .build();
            return this;
        }

        public Builder helperCommand(List<String> helperCommand, Duration timeout) {
            this.quoteProvider = ProcessQuoteProvider.builder()
                    .helperCommand(helperCommand)
                    .timeout(timeout)
                    .build();
            return this;
        }

        public Builder helperCommand(List<String> helperCommand, Duration timeout, Path workingDirectory) {
            this.quoteProvider = ProcessQuoteProvider.builder()
                    .helperCommand(helperCommand)
                    .timeout(timeout)
                    .workingDirectory(workingDirectory)
                    .build();
            return this;
        }

        public AliyunTdxAttestationClient build() {
            return new AliyunTdxAttestationClient(this);
        }
    }
}
