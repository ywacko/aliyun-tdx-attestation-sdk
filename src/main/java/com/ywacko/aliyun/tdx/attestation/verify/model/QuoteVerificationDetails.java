package com.ywacko.aliyun.tdx.attestation.verify.model;

import java.util.Objects;

/**
 * 新证明画像接口使用的验证详情；既有 verifyQuote 仍只返回 QuoteVerificationResult。
 */
public final class QuoteVerificationDetails {

    private final QuoteVerificationResult verification;
    private final AttestationEvidence evidence;

    public QuoteVerificationDetails(QuoteVerificationResult verification, AttestationEvidence evidence) {
        this.verification = Objects.requireNonNull(verification, "verification");
        this.evidence = evidence;
    }

    public QuoteVerificationResult getVerification() {
        return verification;
    }

    public AttestationEvidence getEvidence() {
        return evidence;
    }
}
