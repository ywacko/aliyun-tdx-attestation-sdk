package com.ywacko.aliyun.tdx.attestation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.jna.NativeTdxAttestationApi;
import com.ywacko.aliyun.tdx.attestation.jna.TdxAttestLibrary;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprintReportDataFactory;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import com.ywacko.aliyun.tdx.attestation.model.TdxEnvironmentProfile;
import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;
import com.ywacko.aliyun.tdx.attestation.verify.QuoteEvidenceVerificationResult;
import com.ywacko.aliyun.tdx.attestation.verify.model.AttestationEvidence;
import com.ywacko.aliyun.tdx.attestation.verify.model.AttestationTokenClaims;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationRequest;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;
import com.ywacko.aliyun.tdx.attestation.verify.model.TdxQuoteClaims;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void shouldKeepLegacyGenerateAndVerifyApiSurfaceStable() throws Exception {
        assertEquals(QuoteGenerationResult.class,
                AliyunTdxAttestationClient.class.getMethod("generateQuote", DeploymentFingerprint.class)
                        .getReturnType());
        assertEquals(QuoteGenerationResult.class,
                AliyunTdxAttestationClient.class.getMethod("generateQuote", QuoteGenerationRequest.class)
                        .getReturnType());
        assertEquals(QuoteVerificationResult.class,
                AliyunTdxAttestationClient.class.getMethod("verifyQuote", QuoteVerificationRequest.class)
                        .getReturnType());

        assertGetterNames(QuoteGenerationResult.class, Set.of(
                "getQuoteBytes",
                "getQuoteBase64",
                "getQuoteSha256Hex",
                "getDeploymentDigestHex",
                "getReportDataHex",
                "getQuoteSize",
                "getProvider",
                "getProviderVersion"
        ));
        assertGetterNames(QuoteVerificationRequest.class, Set.of(
                "getService",
                "getImageDigest",
                "getGitRev",
                "getDeploymentDigestHex",
                "getReportDataHex",
                "getQuoteBase64",
                "getQuoteSha256Hex",
                "getQuoteSize",
                "getProvider",
                "getProviderVersion"
        ));
        assertGetterNames(QuoteVerificationResult.class, Set.of(
                "isVerified",
                "getResultCode",
                "getMessage",
                "isStructureValid",
                "isContentValid",
                "isQuoteValid",
                "isQuoteHashMatched",
                "isQuoteSizeMatched",
                "isDeploymentDigestMatched",
                "isReportDataMatched",
                "isAttestedReportDataMatched",
                "isProviderMatched",
                "isProviderVersionMatched",
                "getExpectedDeploymentDigestHex",
                "getActualDeploymentDigestHex",
                "getExpectedReportDataHex",
                "getActualReportDataHex",
                "getAttestedReportDataHex",
                "getVerifierProvider",
                "getVerifierVersion"
        ));
        assertThrows(NoSuchMethodException.class, () -> QuoteVerificationResult.class.getMethod("getEvidence"));
    }

    @Test
    void shouldAttestCurrentEnvironmentWithoutChangingVerifyResultShape() throws Exception {
        byte[] fakeQuote = "quote-bytes".getBytes(StandardCharsets.UTF_8);
        AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
                .nativeApi(new NativeTdxAttestationApi(new FakeTdxAttestLibrary(fakeQuote)))
                .tdxDevicePath(Files.createTempFile("tdx-device", ".mock"))
                .quoteEvidenceVerifier((quoteBytes, expectedReportDataHex) -> QuoteEvidenceVerificationResult.builder()
                        .valid(true)
                        .attestedReportDataHex(expectedReportDataHex)
                        .provider("fake-aliyun-attestation")
                        .providerVersion("test")
                        .evidence(AttestationEvidence.builder()
                                .tokenClaims(AttestationTokenClaims.builder().tee("tdx").build())
                                .tdxQuote(TdxQuoteClaims.builder()
                                        .body(TdxQuoteClaims.Body.builder()
                                                .mrTd("aaaaaaaa")
                                                .reportData(expectedReportDataHex)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        DeploymentFingerprint fingerprint = fingerprint();

        TdxEnvironmentProfile profile = client.attestCurrentEnvironment(fingerprint);

        assertTrue(profile.isVerified());
        assertEquals(fingerprint, profile.getFingerprint());
        assertEquals(JnaQuoteProvider.PROVIDER_VERSION, profile.getQuote().getProviderVersion());
        assertEquals(profile.getQuote().getReportDataHex(), profile.getVerification().getExpectedReportDataHex());
        assertNotNull(profile.getEvidence());
        assertEquals("aaaaaaaa", profile.getEvidence().getTdxQuote().getBody().getMrTd());
    }

    private QuoteVerificationRequest validRequest() {
        DeploymentFingerprint fingerprint = fingerprint();
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

    private DeploymentFingerprint fingerprint() {
        return DeploymentFingerprint.builder()
                .service("tee-gateway")
                .imageDigest("ywackoo/tee-gateway@sha256:3872a935ba90b46925684a818401a682fb1aefd70b397e9c110bbbd2781aef46")
                .gitRev("021b2d7")
                .build();
    }

    private void assertGetterNames(Class<?> type, Set<String> expectedNames) {
        Set<String> actualNames = Arrays.stream(type.getMethods())
                .filter(method -> method.getDeclaringClass().equals(type))
                .filter(method -> method.getParameterCount() == 0)
                .map(Method::getName)
                .filter(name -> name.startsWith("get") || name.startsWith("is"))
                .collect(Collectors.toSet());
        assertEquals(expectedNames, actualNames);
    }

    private static final class FakeTdxAttestLibrary implements TdxAttestLibrary {
        private final byte[] quoteBytes;

        private FakeTdxAttestLibrary(byte[] quoteBytes) {
            this.quoteBytes = quoteBytes.clone();
        }

        @Override
        public int tdx_att_get_quote(TdxReportData reportData,
                                     Pointer attKeyIdList,
                                     int listSize,
                                     Pointer attKeyId,
                                     PointerByReference quoteBuffer,
                                     IntByReference quoteSize,
                                     int flags) {
            Memory memory = new Memory(quoteBytes.length);
            memory.write(0, quoteBytes, 0, quoteBytes.length);
            quoteBuffer.setValue(memory);
            quoteSize.setValue(quoteBytes.length);
            return 0;
        }

        @Override
        public void tdx_att_free_quote(Pointer quoteBuffer) {
            // no-op for the in-memory fake implementation
        }
    }
}
