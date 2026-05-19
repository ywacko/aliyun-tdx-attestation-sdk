package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprintReportDataFactory;
import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;
import com.ywacko.aliyun.tdx.attestation.verify.QuoteEvidenceVerificationResult;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationRequest;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunTdxAttestationClientVerificationTest {

    @Test
    void shouldVerifyWithoutLoadingNativeQuoteProvider() {
        AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
                .libraryName("library-that-must-not-be-loaded-when-verifying")
                .quoteEvidenceVerifier((quoteBytes, expectedReportDataHex) -> QuoteEvidenceVerificationResult.builder()
                        .valid(true)
                        .attestedReportDataHex(expectedReportDataHex)
                        .provider("fake-aliyun-attestation")
                        .providerVersion("test")
                        .build())
                .build();

        QuoteVerificationResult result = client.verifyQuote(validRequest());

        assertTrue(result.isVerified());
    }

    private QuoteVerificationRequest validRequest() {
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
                .providerVersion(JnaQuoteProvider.PROVIDER_VERSION)
                .build();
    }
}
