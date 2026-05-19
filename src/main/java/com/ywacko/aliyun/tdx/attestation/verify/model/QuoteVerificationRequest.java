package com.ywacko.aliyun.tdx.attestation.verify.model;

/**
 * 网关 Quote 输出的 SDK 验证入参。
 * 这里保留网关响应中的完整字段，验证器负责判断哪些字段参与可信校验。
 */
public final class QuoteVerificationRequest {

    private final String service;
    private final String imageDigest;
    private final String gitRev;
    private final String deploymentDigestHex;
    private final String reportDataHex;
    private final String quoteBase64;
    private final String quoteSha256Hex;
    private final Integer quoteSize;
    private final String provider;
    private final String providerVersion;

    private QuoteVerificationRequest(Builder builder) {
        this.service = builder.service;
        this.imageDigest = builder.imageDigest;
        this.gitRev = builder.gitRev;
        this.deploymentDigestHex = builder.deploymentDigestHex;
        this.reportDataHex = builder.reportDataHex;
        this.quoteBase64 = builder.quoteBase64;
        this.quoteSha256Hex = builder.quoteSha256Hex;
        this.quoteSize = builder.quoteSize;
        this.provider = builder.provider;
        this.providerVersion = builder.providerVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getService() {
        return service;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public String getGitRev() {
        return gitRev;
    }

    public String getDeploymentDigestHex() {
        return deploymentDigestHex;
    }

    public String getReportDataHex() {
        return reportDataHex;
    }

    public String getQuoteBase64() {
        return quoteBase64;
    }

    public String getQuoteSha256Hex() {
        return quoteSha256Hex;
    }

    public Integer getQuoteSize() {
        return quoteSize;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public static final class Builder {
        private String service;
        private String imageDigest;
        private String gitRev;
        private String deploymentDigestHex;
        private String reportDataHex;
        private String quoteBase64;
        private String quoteSha256Hex;
        private Integer quoteSize;
        private String provider;
        private String providerVersion;

        private Builder() {
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder imageDigest(String imageDigest) {
            this.imageDigest = imageDigest;
            return this;
        }

        public Builder gitRev(String gitRev) {
            this.gitRev = gitRev;
            return this;
        }

        public Builder deploymentDigestHex(String deploymentDigestHex) {
            this.deploymentDigestHex = deploymentDigestHex;
            return this;
        }

        public Builder reportDataHex(String reportDataHex) {
            this.reportDataHex = reportDataHex;
            return this;
        }

        public Builder quoteBase64(String quoteBase64) {
            this.quoteBase64 = quoteBase64;
            return this;
        }

        public Builder quoteSha256Hex(String quoteSha256Hex) {
            this.quoteSha256Hex = quoteSha256Hex;
            return this;
        }

        public Builder quoteSize(Integer quoteSize) {
            this.quoteSize = quoteSize;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder providerVersion(String providerVersion) {
            this.providerVersion = providerVersion;
            return this;
        }

        public QuoteVerificationRequest build() {
            return new QuoteVerificationRequest(this);
        }
    }
}
