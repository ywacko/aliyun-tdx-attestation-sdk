package com.ywacko.aliyun.tdx.attestation.verify;

import com.ywacko.aliyun.tdx.attestation.verify.model.AttestationEvidence;

/**
 * 外部 Quote 证明验证结果。
 * valid 只表示证明服务接受 Quote；attestedReportDataHex 用于和 SDK 本地期望 report_data 交叉比对。
 */
public final class QuoteEvidenceVerificationResult {

    private final boolean valid;
    private final String attestedReportDataHex;
    private final String provider;
    private final String providerVersion;
    private final String failureReason;
    private final AttestationEvidence evidence;

    private QuoteEvidenceVerificationResult(Builder builder) {
        this.valid = builder.valid;
        this.attestedReportDataHex = builder.attestedReportDataHex;
        this.provider = builder.provider;
        this.providerVersion = builder.providerVersion;
        this.failureReason = builder.failureReason;
        this.evidence = builder.evidence;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static QuoteEvidenceVerificationResult notConfigured() {
        return builder()
                .valid(false)
                .provider("not-configured")
                .failureReason("quote evidence verifier is not configured")
                .build();
    }

    public static QuoteEvidenceVerificationResult notRun(String reason) {
        return builder()
                .valid(false)
                .provider("not-run")
                .failureReason(reason)
                .build();
    }

    public boolean isValid() {
        return valid;
    }

    public String getAttestedReportDataHex() {
        return attestedReportDataHex;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public AttestationEvidence getEvidence() {
        return evidence;
    }

    public static final class Builder {
        private boolean valid;
        private String attestedReportDataHex;
        private String provider;
        private String providerVersion;
        private String failureReason;
        private AttestationEvidence evidence;

        private Builder() {
        }

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder attestedReportDataHex(String attestedReportDataHex) {
            this.attestedReportDataHex = attestedReportDataHex;
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

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder evidence(AttestationEvidence evidence) {
            this.evidence = evidence;
            return this;
        }

        public QuoteEvidenceVerificationResult build() {
            return new QuoteEvidenceVerificationResult(this);
        }
    }
}
