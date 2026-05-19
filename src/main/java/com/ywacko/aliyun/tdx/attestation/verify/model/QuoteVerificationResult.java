package com.ywacko.aliyun.tdx.attestation.verify.model;

/**
 * Quote 验证结果。
 * verified 是总结果；其余字段用于解释每一层校验是否通过。
 */
public final class QuoteVerificationResult {

    private final boolean verified;
    private final String resultCode;
    private final String message;
    private final boolean structureValid;
    private final boolean contentValid;
    private final boolean quoteValid;
    private final boolean quoteHashMatched;
    private final boolean quoteSizeMatched;
    private final boolean deploymentDigestMatched;
    private final boolean reportDataMatched;
    private final boolean attestedReportDataMatched;
    private final boolean providerMatched;
    private final boolean providerVersionMatched;
    private final String expectedDeploymentDigestHex;
    private final String actualDeploymentDigestHex;
    private final String expectedReportDataHex;
    private final String actualReportDataHex;
    private final String attestedReportDataHex;
    private final String verifierProvider;
    private final String verifierVersion;

    private QuoteVerificationResult(Builder builder) {
        this.verified = builder.verified;
        this.resultCode = builder.resultCode;
        this.message = builder.message;
        this.structureValid = builder.structureValid;
        this.contentValid = builder.contentValid;
        this.quoteValid = builder.quoteValid;
        this.quoteHashMatched = builder.quoteHashMatched;
        this.quoteSizeMatched = builder.quoteSizeMatched;
        this.deploymentDigestMatched = builder.deploymentDigestMatched;
        this.reportDataMatched = builder.reportDataMatched;
        this.attestedReportDataMatched = builder.attestedReportDataMatched;
        this.providerMatched = builder.providerMatched;
        this.providerVersionMatched = builder.providerVersionMatched;
        this.expectedDeploymentDigestHex = builder.expectedDeploymentDigestHex;
        this.actualDeploymentDigestHex = builder.actualDeploymentDigestHex;
        this.expectedReportDataHex = builder.expectedReportDataHex;
        this.actualReportDataHex = builder.actualReportDataHex;
        this.attestedReportDataHex = builder.attestedReportDataHex;
        this.verifierProvider = builder.verifierProvider;
        this.verifierVersion = builder.verifierVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isVerified() {
        return verified;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public boolean isContentValid() {
        return contentValid;
    }

    public boolean isQuoteValid() {
        return quoteValid;
    }

    public boolean isQuoteHashMatched() {
        return quoteHashMatched;
    }

    public boolean isQuoteSizeMatched() {
        return quoteSizeMatched;
    }

    public boolean isDeploymentDigestMatched() {
        return deploymentDigestMatched;
    }

    public boolean isReportDataMatched() {
        return reportDataMatched;
    }

    public boolean isAttestedReportDataMatched() {
        return attestedReportDataMatched;
    }

    public boolean isProviderMatched() {
        return providerMatched;
    }

    public boolean isProviderVersionMatched() {
        return providerVersionMatched;
    }

    public String getExpectedDeploymentDigestHex() {
        return expectedDeploymentDigestHex;
    }

    public String getActualDeploymentDigestHex() {
        return actualDeploymentDigestHex;
    }

    public String getExpectedReportDataHex() {
        return expectedReportDataHex;
    }

    public String getActualReportDataHex() {
        return actualReportDataHex;
    }

    public String getAttestedReportDataHex() {
        return attestedReportDataHex;
    }

    public String getVerifierProvider() {
        return verifierProvider;
    }

    public String getVerifierVersion() {
        return verifierVersion;
    }

    public static final class Builder {
        private boolean verified;
        private String resultCode;
        private String message;
        private boolean structureValid;
        private boolean contentValid;
        private boolean quoteValid;
        private boolean quoteHashMatched;
        private boolean quoteSizeMatched;
        private boolean deploymentDigestMatched;
        private boolean reportDataMatched;
        private boolean attestedReportDataMatched;
        private boolean providerMatched;
        private boolean providerVersionMatched;
        private String expectedDeploymentDigestHex;
        private String actualDeploymentDigestHex;
        private String expectedReportDataHex;
        private String actualReportDataHex;
        private String attestedReportDataHex;
        private String verifierProvider;
        private String verifierVersion;

        private Builder() {
        }

        public Builder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder resultCode(String resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder structureValid(boolean structureValid) {
            this.structureValid = structureValid;
            return this;
        }

        public Builder contentValid(boolean contentValid) {
            this.contentValid = contentValid;
            return this;
        }

        public Builder quoteValid(boolean quoteValid) {
            this.quoteValid = quoteValid;
            return this;
        }

        public Builder quoteHashMatched(boolean quoteHashMatched) {
            this.quoteHashMatched = quoteHashMatched;
            return this;
        }

        public Builder quoteSizeMatched(boolean quoteSizeMatched) {
            this.quoteSizeMatched = quoteSizeMatched;
            return this;
        }

        public Builder deploymentDigestMatched(boolean deploymentDigestMatched) {
            this.deploymentDigestMatched = deploymentDigestMatched;
            return this;
        }

        public Builder reportDataMatched(boolean reportDataMatched) {
            this.reportDataMatched = reportDataMatched;
            return this;
        }

        public Builder attestedReportDataMatched(boolean attestedReportDataMatched) {
            this.attestedReportDataMatched = attestedReportDataMatched;
            return this;
        }

        public Builder providerMatched(boolean providerMatched) {
            this.providerMatched = providerMatched;
            return this;
        }

        public Builder providerVersionMatched(boolean providerVersionMatched) {
            this.providerVersionMatched = providerVersionMatched;
            return this;
        }

        public Builder expectedDeploymentDigestHex(String expectedDeploymentDigestHex) {
            this.expectedDeploymentDigestHex = expectedDeploymentDigestHex;
            return this;
        }

        public Builder actualDeploymentDigestHex(String actualDeploymentDigestHex) {
            this.actualDeploymentDigestHex = actualDeploymentDigestHex;
            return this;
        }

        public Builder expectedReportDataHex(String expectedReportDataHex) {
            this.expectedReportDataHex = expectedReportDataHex;
            return this;
        }

        public Builder actualReportDataHex(String actualReportDataHex) {
            this.actualReportDataHex = actualReportDataHex;
            return this;
        }

        public Builder attestedReportDataHex(String attestedReportDataHex) {
            this.attestedReportDataHex = attestedReportDataHex;
            return this;
        }

        public Builder verifierProvider(String verifierProvider) {
            this.verifierProvider = verifierProvider;
            return this;
        }

        public Builder verifierVersion(String verifierVersion) {
            this.verifierVersion = verifierVersion;
            return this;
        }

        public QuoteVerificationResult build() {
            return new QuoteVerificationResult(this);
        }
    }
}
