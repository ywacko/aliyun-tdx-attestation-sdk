package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprintReportDataFactory;
import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;
import com.ywacko.aliyun.tdx.attestation.verify.DefaultQuoteVerifier;
import com.ywacko.aliyun.tdx.attestation.verify.QuoteEvidenceVerificationResult;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationRequest;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultQuoteVerifierTest {

    @Test
    void shouldVerifyGatewayQuoteOutputWhenEvidenceVerifierPasses() {
        QuoteVerificationRequest request = validRequestBuilder().build();
        DefaultQuoteVerifier verifier = DefaultQuoteVerifier.builder()
                .evidenceVerifier((quoteBytes, expectedReportDataHex) -> QuoteEvidenceVerificationResult.builder()
                        .valid(true)
                        .attestedReportDataHex(expectedReportDataHex)
                        .provider("fake-aliyun-attestation")
                        .providerVersion("test")
                        .build())
                .build();

        QuoteVerificationResult result = verifier.verify(request);

        assertTrue(result.isVerified());
        assertEquals("PASSED", result.getResultCode());
        assertTrue(result.isStructureValid());
        assertTrue(result.isContentValid());
        assertTrue(result.isQuoteValid());
        assertTrue(result.isQuoteHashMatched());
        assertTrue(result.isQuoteSizeMatched());
        assertTrue(result.isDeploymentDigestMatched());
        assertTrue(result.isReportDataMatched());
        assertTrue(result.isAttestedReportDataMatched());
        assertTrue(result.isProviderMatched());
        assertTrue(result.isProviderVersionMatched());
        assertEquals(request.getDeploymentDigestHex(), result.getExpectedDeploymentDigestHex());
        assertEquals(request.getReportDataHex(), result.getExpectedReportDataHex());
    }

    @Test
    void shouldNotPassWhenEvidenceVerifierFails() {
        QuoteVerificationResult result = DefaultQuoteVerifier.builder()
                .evidenceVerifier((quoteBytes, expectedReportDataHex) ->
                        QuoteEvidenceVerificationResult.notConfigured())
                .build()
                .verify(validRequestBuilder().build());

        assertFalse(result.isVerified());
        assertFalse(result.isQuoteValid());
        assertEquals("QUOTE_EVIDENCE_VERIFIER_NOT_CONFIGURED", result.getResultCode());
    }

    @Test
    void shouldRejectUnexpectedProviderVersion() {
        QuoteVerificationRequest request = validRequestBuilder()
                .providerVersion("v1.0.1")
                .build();
        DefaultQuoteVerifier verifier = DefaultQuoteVerifier.builder()
                .evidenceVerifier((quoteBytes, expectedReportDataHex) -> QuoteEvidenceVerificationResult.builder()
                        .valid(true)
                        .attestedReportDataHex(expectedReportDataHex)
                        .provider("fake-aliyun-attestation")
                        .providerVersion("test")
                        .build())
                .build();

        QuoteVerificationResult result = verifier.verify(request);

        assertFalse(result.isVerified());
        assertFalse(result.isProviderVersionMatched());
        assertEquals("PROVIDER_VERSION_MISMATCH", result.getResultCode());
    }

    @Test
    void shouldRejectDeploymentDigestMismatch() {
        QuoteVerificationRequest request = validRequestBuilder()
                .deploymentDigestHex("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .build();
        DefaultQuoteVerifier verifier = DefaultQuoteVerifier.builder()
                .evidenceVerifier((quoteBytes, expectedReportDataHex) -> QuoteEvidenceVerificationResult.builder()
                        .valid(true)
                        .attestedReportDataHex(expectedReportDataHex)
                        .provider("fake-aliyun-attestation")
                        .providerVersion("test")
                        .build())
                .build();

        QuoteVerificationResult result = verifier.verify(request);

        assertFalse(result.isVerified());
        assertFalse(result.isDeploymentDigestMatched());
        assertEquals("DEPLOYMENT_DIGEST_MISMATCH", result.getResultCode());
    }

    private QuoteVerificationRequest.Builder validRequestBuilder() {
        DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
                .service("tee-gateway")
                .imageDigest("ywackoo/tee-gateway@sha256:3872a935ba90b46925684a818401a682fb1aefd70b397e9c110bbbd2781aef46")
                .gitRev("021b2d7")
                .build();
        byte[] quoteBytes = "quote-bytes".getBytes();

        return QuoteVerificationRequest.builder()
                .service(fingerprint.getService())
                .imageDigest(fingerprint.getImageDigest())
                .gitRev(fingerprint.getGitRev())
                .deploymentDigestHex(DeploymentFingerprintReportDataFactory.digestHex(fingerprint))
                .reportDataHex(DeploymentFingerprintReportDataFactory.reportData(fingerprint).toHex())
                .quoteBase64(Base64.getEncoder().encodeToString(quoteBytes))
                .quoteSha256Hex(HexUtils.toHex(Sha256Utils.sha256(quoteBytes)))
                .quoteSize(quoteBytes.length)
                .provider(JnaQuoteProvider.PROVIDER)
                .providerVersion(JnaQuoteProvider.PROVIDER_VERSION);
    }
}
