package com.ywacko.aliyun.tdx.attestation;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.verify.AliyunRemoteAttestationConfig;
import com.ywacko.aliyun.tdx.attestation.verify.AliyunRemoteQuoteEvidenceVerifier;
import com.ywacko.aliyun.tdx.attestation.verify.QuoteEvidenceVerificationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunRemoteQuoteEvidenceVerifierTest {

    @Test
    void shouldCallAliyunStyleEndpointVerifyJwtAndExtractReportData() throws Exception {
        byte[] quoteBytes = "quote-bytes".getBytes(StandardCharsets.UTF_8);
        String reportDataHex = "4524ec23aa0c756e663043f153eb075d1aeb74e8a43e596e1ba6f283e68473b4"
                + "0000000000000000000000000000000000000000000000000000000000000000";
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        RSAKey rsaKey = new RSAKeyGenerator(2048)
                .keyID("test-kid")
                .algorithm(JWSAlgorithm.RS256)
                .generate();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/jwks.json", exchange ->
                    writeResponse(exchange, 200, new JWKSet(rsaKey.toPublicJWK()).toString()));
            server.createContext("/v1/attestation", exchange -> {
                try {
                    String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    assertAttestationRequestCarriesQuote(requestBody, quoteBytes);
                    writeResponse(exchange, 200, signedJwt(rsaKey, now, reportDataHex));
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            });
            server.start();

            String baseUri = "http://127.0.0.1:" + server.getAddress().getPort();
            AliyunRemoteAttestationConfig config = AliyunRemoteAttestationConfig.builder()
                    .attestationEndpoint(java.net.URI.create(baseUri + "/v1/attestation"))
                    .jwksUri(java.net.URI.create(baseUri + "/jwks.json"))
                    .issuer("https://attest.cn-beijing.aliyuncs.com")
                    .audience("https://attest.cn-beijing.aliyuncs.com")
                    .build();
            AliyunRemoteQuoteEvidenceVerifier verifier = AliyunRemoteQuoteEvidenceVerifier.builder()
                    .config(config)
                    .clock(Clock.fixed(now, ZoneOffset.UTC))
                    .build();

            QuoteEvidenceVerificationResult result = verifier.verify(quoteBytes, reportDataHex);

            assertTrue(result.isValid());
            assertEquals(HexUtils.normalize(reportDataHex), result.getAttestedReportDataHex());
            assertEquals(AliyunRemoteQuoteEvidenceVerifier.PROVIDER, result.getProvider());
            assertEquals(AliyunRemoteQuoteEvidenceVerifier.PROVIDER_VERSION, result.getProviderVersion());
        } finally {
            server.stop(0);
        }
    }

    @SuppressWarnings("unchecked")
    private void assertAttestationRequestCarriesQuote(String requestBody, byte[] quoteBytes) throws ParseException {
        Map<String, Object> requestJson = JSONObjectUtils.parse(requestBody);
        assertEquals("tdx", requestJson.get("tee"));
        String evidence = (String) requestJson.get("evidence");
        String evidenceJson = new String(Base64.getUrlDecoder().decode(evidence), StandardCharsets.UTF_8);
        Map<String, Object> decodedEvidence = JSONObjectUtils.parse(evidenceJson);
        assertEquals(Base64.getEncoder().encodeToString(quoteBytes), decodedEvidence.get("quote"));
    }

    private String signedJwt(RSAKey rsaKey, Instant now, String reportDataHex) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("https://attest.cn-beijing.aliyuncs.com")
                .audience("https://attest.cn-beijing.aliyuncs.com")
                .issueTime(Date.from(now.minusSeconds(10)))
                .notBeforeTime(Date.from(now.minusSeconds(10)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .jwtID("jwt-id")
                .claim("eat_profile", "https://www.alibabacloud.com/help/en/ecs/user-guide/eat-profile")
                .claim("intuse", "generic")
                .claim("tee", "tdx")
                .claim("tcb-status", "{\"tdx.quote.body.report_data\":\"" + reportDataHex + "\"}")
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }

    private static void writeResponse(com.sun.net.httpserver.HttpExchange exchange,
                                      int statusCode,
                                      String responseBody) throws IOException {
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
