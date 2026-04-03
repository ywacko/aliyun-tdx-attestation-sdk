package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.process.QuoteProvider;

import java.nio.file.Path;
import java.util.Objects;

public final class AliyunTdxAttestationClient {

    private final QuoteProvider quoteProvider;

    private AliyunTdxAttestationClient(Builder builder) {
        this.quoteProvider = builder.quoteProvider != null
                ? builder.quoteProvider
                : JnaQuoteProvider.builder().build();
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
        private final JnaQuoteProvider.Builder jnaBuilder = JnaQuoteProvider.builder();

        private Builder() {
        }

        public Builder quoteProvider(QuoteProvider quoteProvider) {
            this.quoteProvider = quoteProvider;
            return this;
        }

        public Builder useJna() {
            this.quoteProvider = jnaBuilder.build();
            return this;
        }

        public Builder libraryName(String libraryName) {
            this.quoteProvider = jnaBuilder.libraryName(libraryName).build();
            return this;
        }

        public Builder tdxDevicePath(Path tdxDevicePath) {
            this.quoteProvider = jnaBuilder.tdxDevicePath(tdxDevicePath).build();
            return this;
        }

        public AliyunTdxAttestationClient build() {
            return new AliyunTdxAttestationClient(this);
        }
    }
}
