package com.ywacko.aliyun.tdx.attestation.jna;

import com.ywacko.aliyun.tdx.attestation.exception.QuoteGenerationException;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import com.ywacko.aliyun.tdx.attestation.process.QuoteProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class JnaQuoteProvider implements QuoteProvider {

    private final NativeTdxAttestationApi nativeApi;
    private final Path tdxDevicePath;

    private JnaQuoteProvider(Builder builder) {
        this.nativeApi = Objects.requireNonNull(builder.nativeApi, "nativeApi");
        this.tdxDevicePath = builder.tdxDevicePath;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public QuoteGenerationResult generateQuote(QuoteGenerationRequest request) {
        Objects.requireNonNull(request, "request");
        validateEnvironment();

        byte[] quoteBytes = nativeApi.getQuote(request.getReportData().getBytes());
        return new QuoteGenerationResult(
                quoteBytes,
                request.getDeploymentDigestHex(),
                request.getReportData().toHex(),
                quoteBytes.length,
                "aliyun-tdx-jna",
                null
        );
    }

    private void validateEnvironment() {
        if (!Files.exists(tdxDevicePath)) {
            throw new QuoteGenerationException("TDX device not found: " + tdxDevicePath
                    + ". This SDK must run on an Aliyun TDX VM with /dev/tdx_guest available.");
        }
    }

    public static final class Builder {
        private NativeTdxAttestationApi nativeApi;
        private String libraryName = "tdx_attest";
        private Path tdxDevicePath = Path.of("/dev/tdx_guest");

        private Builder() {
        }

        public Builder nativeApi(NativeTdxAttestationApi nativeApi) {
            this.nativeApi = nativeApi;
            return this;
        }

        public Builder libraryName(String libraryName) {
            this.libraryName = libraryName;
            return this;
        }

        public Builder tdxDevicePath(Path tdxDevicePath) {
            this.tdxDevicePath = tdxDevicePath;
            return this;
        }

        public JnaQuoteProvider build() {
            if (nativeApi == null) {
                nativeApi = new NativeTdxAttestationApi(TdxAttestLibrary.load(libraryName));
            }
            return new JnaQuoteProvider(this);
        }
    }
}
