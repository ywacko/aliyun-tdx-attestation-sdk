package com.ywacko.aliyun.tdx.attestation.verify;

import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprintReportDataFactory;
import com.ywacko.aliyun.tdx.attestation.model.ReportData;
import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationRequest;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;

import java.util.Base64;

/**
 * SDK 默认 Quote 验证器。
 * 本类先做 SDK 字段自洽校验，再通过 QuoteEvidenceVerifier 承接真正的证明服务校验。
 */
public final class DefaultQuoteVerifier implements QuoteVerifier {

    public static final String VERIFIER_PROVIDER = "aliyun-tdx-sdk-local-verifier";
    public static final String VERIFIER_VERSION = "v2.0.0";

    private final QuoteEvidenceVerifier evidenceVerifier;
    private final String expectedProvider;
    private final String expectedProviderVersion;

    private DefaultQuoteVerifier(Builder builder) {
        this.evidenceVerifier = builder.evidenceVerifier;
        this.expectedProvider = builder.expectedProvider;
        this.expectedProviderVersion = builder.expectedProviderVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public QuoteVerificationResult verify(QuoteVerificationRequest request) {
        if (request == null) {
            return failed("INPUT_INVALID", "request must not be null");
        }

        DeploymentFingerprint fingerprint;
        try {
            fingerprint = DeploymentFingerprint.builder()
                    .service(request.getService())
                    .imageDigest(request.getImageDigest())
                    .gitRev(request.getGitRev())
                    .build();
        } catch (RuntimeException ex) {
            return failed("INPUT_INVALID", ex.getMessage());
        }

        String expectedDeploymentDigestHex = DeploymentFingerprintReportDataFactory.digestHex(fingerprint);
        String expectedReportDataHex = ReportData
                .fromDeploymentDigest(HexUtils.fromHex(expectedDeploymentDigestHex))
                .toHex();
        String actualDeploymentDigestHex = normalizeHexOrNull(request.getDeploymentDigestHex());
        String actualReportDataHex = normalizeHexOrNull(request.getReportDataHex());

        byte[] quoteBytes = decodeQuoteOrNull(request.getQuoteBase64());
        String actualQuoteHashHex = normalizeHexOrNull(request.getQuoteSha256Hex());

        boolean quoteHashMatched = quoteBytes != null
                && actualQuoteHashHex != null
                && HexUtils.toHex(Sha256Utils.sha256(quoteBytes)).equals(actualQuoteHashHex);
        boolean quoteSizeMatched = quoteBytes != null
                && request.getQuoteSize() != null
                && request.getQuoteSize() == quoteBytes.length;
        boolean deploymentDigestMatched = expectedDeploymentDigestHex.equals(actualDeploymentDigestHex);
        boolean localReportDataMatched = expectedReportDataHex.equals(actualReportDataHex);
        boolean providerMatched = expectedProvider.equals(request.getProvider());
        boolean providerVersionMatched = expectedProviderVersion.equals(request.getProviderVersion());
        boolean structureValid = quoteBytes != null
                && quoteHashMatched
                && quoteSizeMatched
                && deploymentDigestMatched
                && localReportDataMatched
                && providerMatched
                && providerVersionMatched;

        QuoteEvidenceVerificationResult evidenceResult = structureValid
                ? evidenceVerifier.verify(quoteBytes, expectedReportDataHex)
                : QuoteEvidenceVerificationResult.notRun("local quote fields are not valid");

        String attestedReportDataHex = normalizeHexOrNull(evidenceResult.getAttestedReportDataHex());
        boolean attestedReportDataMatched = expectedReportDataHex.equals(attestedReportDataHex);
        boolean quoteValid = evidenceResult.isValid();
        boolean contentValid = quoteValid && attestedReportDataMatched;
        boolean reportDataMatched = localReportDataMatched && contentValid;

        boolean verified = structureValid && contentValid;

        String resultCode = verified ? "PASSED" : firstFailureCode(
                quoteBytes,
                quoteHashMatched,
                quoteSizeMatched,
                deploymentDigestMatched,
                localReportDataMatched,
                providerMatched,
                providerVersionMatched,
                quoteValid,
                attestedReportDataMatched,
                evidenceResult
        );

        return QuoteVerificationResult.builder()
                .verified(verified)
                .resultCode(resultCode)
                .message(verified ? "quote verification passed" : failureMessage(resultCode, evidenceResult))
                .structureValid(structureValid)
                .contentValid(contentValid)
                .quoteValid(quoteValid)
                .quoteHashMatched(quoteHashMatched)
                .quoteSizeMatched(quoteSizeMatched)
                .deploymentDigestMatched(deploymentDigestMatched)
                .reportDataMatched(reportDataMatched)
                .attestedReportDataMatched(attestedReportDataMatched)
                .providerMatched(providerMatched)
                .providerVersionMatched(providerVersionMatched)
                .expectedDeploymentDigestHex(expectedDeploymentDigestHex)
                .actualDeploymentDigestHex(actualDeploymentDigestHex)
                .expectedReportDataHex(expectedReportDataHex)
                .actualReportDataHex(actualReportDataHex)
                .attestedReportDataHex(attestedReportDataHex)
                .verifierProvider(evidenceResult.getProvider())
                .verifierVersion(evidenceResult.getProviderVersion())
                .build();
    }

    private QuoteVerificationResult failed(String resultCode, String message) {
        return QuoteVerificationResult.builder()
                .verified(false)
                .resultCode(resultCode)
                .message(message)
                .verifierProvider(VERIFIER_PROVIDER)
                .verifierVersion(VERIFIER_VERSION)
                .build();
    }

    private String firstFailureCode(byte[] quoteBytes,
                                    boolean quoteHashMatched,
                                    boolean quoteSizeMatched,
                                    boolean deploymentDigestMatched,
                                    boolean localReportDataMatched,
                                    boolean providerMatched,
                                    boolean providerVersionMatched,
                                    boolean quoteValid,
                                    boolean attestedReportDataMatched,
                                    QuoteEvidenceVerificationResult evidenceResult) {
        if (quoteBytes == null) {
            return "QUOTE_BASE64_INVALID";
        }
        if (!quoteHashMatched) {
            return "QUOTE_HASH_MISMATCH";
        }
        if (!quoteSizeMatched) {
            return "QUOTE_SIZE_MISMATCH";
        }
        if (!deploymentDigestMatched) {
            return "DEPLOYMENT_DIGEST_MISMATCH";
        }
        if (!localReportDataMatched) {
            return "REPORT_DATA_MISMATCH";
        }
        if (!providerMatched) {
            return "PROVIDER_MISMATCH";
        }
        if (!providerVersionMatched) {
            return "PROVIDER_VERSION_MISMATCH";
        }
        if (!quoteValid) {
            if ("not-configured".equals(evidenceResult.getProvider())) {
                return "QUOTE_EVIDENCE_VERIFIER_NOT_CONFIGURED";
            }
            return "QUOTE_EVIDENCE_INVALID";
        }
        if (!attestedReportDataMatched) {
            return "ATTESTED_REPORT_DATA_MISMATCH";
        }
        return "FAILED";
    }

    private String failureMessage(String resultCode, QuoteEvidenceVerificationResult evidenceResult) {
        if (evidenceResult.getFailureReason() != null
                && (resultCode.startsWith("QUOTE_EVIDENCE") || resultCode.startsWith("ATTESTED_"))) {
            return evidenceResult.getFailureReason();
        }
        return resultCode.toLowerCase();
    }

    private byte[] decodeQuoteOrNull(String quoteBase64) {
        if (quoteBase64 == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(quoteBase64);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeHexOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return HexUtils.normalize(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static final class Builder {
        private QuoteEvidenceVerifier evidenceVerifier = AliyunRemoteQuoteEvidenceVerifier.createDefault();
        private String expectedProvider = JnaQuoteProvider.PROVIDER;
        private String expectedProviderVersion = JnaQuoteProvider.PROVIDER_VERSION;

        private Builder() {
        }

        public Builder evidenceVerifier(QuoteEvidenceVerifier evidenceVerifier) {
            this.evidenceVerifier = evidenceVerifier;
            return this;
        }

        public Builder expectedProvider(String expectedProvider) {
            this.expectedProvider = expectedProvider;
            return this;
        }

        public Builder expectedProviderVersion(String expectedProviderVersion) {
            this.expectedProviderVersion = expectedProviderVersion;
            return this;
        }

        public DefaultQuoteVerifier build() {
            return new DefaultQuoteVerifier(this);
        }
    }
}
