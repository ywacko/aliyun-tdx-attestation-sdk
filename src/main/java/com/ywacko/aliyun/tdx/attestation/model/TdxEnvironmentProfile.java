package com.ywacko.aliyun.tdx.attestation.model;

import com.ywacko.aliyun.tdx.attestation.verify.model.AttestationEvidence;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;

import java.time.Instant;
import java.util.Objects;

/**
 * SDK 对当前 TDX 运行环境完成生成与验证后的完整证明画像。
 */
public final class TdxEnvironmentProfile {

    private final DeploymentFingerprint fingerprint;
    private final QuoteGenerationResult quote;
    private final QuoteVerificationResult verification;
    private final AttestationEvidence evidence;
    private final Instant attestedAt;

    public TdxEnvironmentProfile(DeploymentFingerprint fingerprint,
                                 QuoteGenerationResult quote,
                                 QuoteVerificationResult verification,
                                 AttestationEvidence evidence,
                                 Instant attestedAt) {
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.quote = Objects.requireNonNull(quote, "quote");
        this.verification = Objects.requireNonNull(verification, "verification");
        this.evidence = evidence;
        this.attestedAt = Objects.requireNonNull(attestedAt, "attestedAt");
    }

    public DeploymentFingerprint getFingerprint() {
        return fingerprint;
    }

    public QuoteGenerationResult getQuote() {
        return quote;
    }

    public QuoteVerificationResult getVerification() {
        return verification;
    }

    public Instant getAttestedAt() {
        return attestedAt;
    }

    public boolean isVerified() {
        return verification.isVerified();
    }

    public AttestationEvidence getEvidence() {
        return evidence;
    }
}
