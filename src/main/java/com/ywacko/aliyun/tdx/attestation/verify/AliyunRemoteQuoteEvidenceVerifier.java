package com.ywacko.aliyun.tdx.attestation.verify;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.verify.model.AttestationEvidence;
import com.ywacko.aliyun.tdx.attestation.verify.model.AttestationTokenClaims;
import com.ywacko.aliyun.tdx.attestation.verify.model.TdxQuoteClaims;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 基于阿里云官方远程证明服务的 Quote 证明验证器。
 */
public final class AliyunRemoteQuoteEvidenceVerifier implements QuoteEvidenceVerifier {

    public static final String PROVIDER = "aliyun-remote-attestation";
    public static final String PROVIDER_VERSION = "v3.0.0";

    private static final String TCB_STATUS_CLAIM = "tcb-status";
    private static final String REPORT_DATA_CLAIM = "tdx.quote.body.report_data";

    private final AliyunRemoteAttestationConfig config;
    private final HttpClient httpClient;
    private final Clock clock;

    private AliyunRemoteQuoteEvidenceVerifier(Builder builder) {
        this.config = builder.config;
        this.httpClient = builder.httpClient;
        this.clock = builder.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AliyunRemoteQuoteEvidenceVerifier createDefault() {
        return builder().build();
    }

    @Override
    public QuoteEvidenceVerificationResult verify(byte[] quoteBytes, String expectedReportDataHex) {
        if (quoteBytes == null || quoteBytes.length == 0) {
            return failure("quote bytes must not be empty");
        }

        try {
            String jwt = requestAttestationToken(quoteBytes);
            AttestationEvidence evidence = verifyJwtAndExtractEvidence(jwt);
            return QuoteEvidenceVerificationResult.builder()
                    .valid(true)
                    .attestedReportDataHex(evidence.getAttestedReportDataHex())
                    .provider(PROVIDER)
                    .providerVersion(PROVIDER_VERSION)
                    .evidence(evidence)
                    .build();
        } catch (RuntimeException ex) {
            return failure(ex.getMessage());
        }
    }

    private String requestAttestationToken(byte[] quoteBytes) {
        String quoteBase64 = Base64.getEncoder().encodeToString(quoteBytes);
        String evidenceJson = "{\"quote\":\"" + quoteBase64 + "\"}";
        String evidence = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(evidenceJson.getBytes(StandardCharsets.UTF_8));

        String requestBody = "{\"evidence\":\"" + evidence + "\",\"tee\":\"tdx\",\"policy_ids\":"
                + policyIdsJson(config.getPolicyIds()) + "}";

        HttpRequest request = HttpRequest.newBuilder(config.getAttestationEndpoint())
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("aliyun attestation request failed: http "
                        + response.statusCode() + ", body=" + trimForMessage(response.body()));
            }
            String body = response.body() == null ? "" : response.body().trim();
            if (body.isEmpty()) {
                throw new IllegalStateException("aliyun attestation response is empty");
            }
            return body;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to call aliyun attestation endpoint: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("aliyun attestation request was interrupted", ex);
        }
    }

    private AttestationEvidence verifyJwtAndExtractEvidence(String jwtText) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtText);
            if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())) {
                throw new IllegalStateException("unexpected aliyun attestation jwt alg: "
                        + signedJWT.getHeader().getAlgorithm());
            }

            JWK jwk = loadJwk(signedJWT.getHeader().getKeyID());
            if (!(jwk instanceof RSAKey)) {
                throw new IllegalStateException("aliyun attestation jwk not found for kid: "
                        + signedJWT.getHeader().getKeyID());
            }

            JWSVerifier verifier = new RSASSAVerifier(((RSAKey) jwk).toRSAPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new IllegalStateException("aliyun attestation jwt signature is invalid");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            validateRegisteredClaims(claims);

            String tee = claims.getStringClaim("tee");
            if (!"tdx".equals(tee)) {
                throw new IllegalStateException("unexpected aliyun attestation tee claim: " + tee);
            }

            Map<String, Object> tcbClaims = parseTcbStatusClaims(claims);
            TdxQuoteClaims tdxQuote = buildTdxQuoteClaims(tcbClaims);
            if (tdxQuote.getBody() == null || tdxQuote.getBody().getReportData() == null) {
                throw new IllegalStateException("aliyun attestation jwt missing " + REPORT_DATA_CLAIM);
            }

            return AttestationEvidence.builder()
                    .rawJwt(jwtText)
                    .tokenClaims(buildTokenClaims(signedJWT, claims))
                    .tdxQuote(tdxQuote)
                    .rawClaims(claims.getClaims())
                    .tcbStatusClaims(tcbClaims)
                    .evaluationReports(claims.getClaim("evaluation-reports"))
                    .customizedClaims(claims.getClaim("customized_claims"))
                    .build();
        } catch (ParseException | JOSEException ex) {
            throw new IllegalStateException("failed to verify aliyun attestation jwt: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> parseTcbStatusClaims(JWTClaimsSet claims) throws ParseException {
        Object tcbStatus = claims.getClaim(TCB_STATUS_CLAIM);
        if (tcbStatus == null) {
            throw new IllegalStateException("aliyun attestation jwt missing tcb-status claim");
        }
        if (tcbStatus instanceof String) {
            String text = ((String) tcbStatus).trim();
            if (text.isEmpty()) {
                throw new IllegalStateException("aliyun attestation jwt missing tcb-status claim");
            }
            return JSONObjectUtils.parse(text);
        }
        throw new IllegalStateException("unexpected aliyun attestation tcb-status claim type: "
                + tcbStatus.getClass().getName());
    }

    private AttestationTokenClaims buildTokenClaims(SignedJWT signedJWT, JWTClaimsSet claims) {
        Map<String, Object> rawClaims = claims.getClaims();
        return AttestationTokenClaims.builder()
                .keyId(signedJWT.getHeader().getKeyID())
                .algorithm(signedJWT.getHeader().getAlgorithm().getName())
                .issuer(claims.getIssuer())
                .audience(claims.getAudience())
                .issuedAt(toInstant(claims.getIssueTime()))
                .notBefore(toInstant(claims.getNotBeforeTime()))
                .expiresAt(toInstant(claims.getExpirationTime()))
                .jwtId(claims.getJWTID())
                .eatProfile(readString(rawClaims, "eat_profile"))
                .intendedUse(readString(rawClaims, "intuse"))
                .tee(readString(rawClaims, "tee"))
                .acsVersion(readString(rawClaims, "x-acs-ver"))
                .build();
    }

    private TdxQuoteClaims buildTdxQuoteClaims(Map<String, Object> tcbClaims) {
        TdxQuoteClaims.Header header = TdxQuoteClaims.Header.builder()
                .version(readHex(tcbClaims, "tdx.quote.header.version"))
                .attKeyType(readHex(tcbClaims, "tdx.quote.header.att_key_type"))
                .teeType(readHex(tcbClaims, "tdx.quote.header.tee_type"))
                .reserved(readHex(tcbClaims, "tdx.quote.header.reserved"))
                .vendorId(readHex(tcbClaims, "tdx.quote.header.vendor_id"))
                .userData(readHex(tcbClaims, "tdx.quote.header.user_data"))
                .build();

        TdxQuoteClaims.Body body = TdxQuoteClaims.Body.builder()
                .tcbSvn(readHex(tcbClaims, "tdx.quote.body.tcb_svn"))
                .mrSeam(readHex(tcbClaims, "tdx.quote.body.mr_seam"))
                .mrSignerSeam(readHex(tcbClaims, "tdx.quote.body.mrsigner_seam"))
                .seamAttributes(readHex(tcbClaims, "tdx.quote.body.seam_attributes"))
                .tdAttributes(readHex(tcbClaims, "tdx.quote.body.td_attributes"))
                .xfam(readHex(tcbClaims, "tdx.quote.body.xfam"))
                .mrTd(readHex(tcbClaims, "tdx.quote.body.mr_td"))
                .mrConfigId(readHex(tcbClaims, "tdx.quote.body.mr_config_id"))
                .mrOwner(readHex(tcbClaims, "tdx.quote.body.mr_owner"))
                .mrOwnerConfig(readHex(tcbClaims, "tdx.quote.body.mr_owner_config"))
                .rtmr0(readHex(tcbClaims, "tdx.quote.body.rtmr_0"))
                .rtmr1(readHex(tcbClaims, "tdx.quote.body.rtmr_1"))
                .rtmr2(readHex(tcbClaims, "tdx.quote.body.rtmr_2"))
                .rtmr3(readHex(tcbClaims, "tdx.quote.body.rtmr_3"))
                .reportData(readHex(tcbClaims, REPORT_DATA_CLAIM))
                .teeTcbSvn2(readHex(tcbClaims, "tdx.quote.body.tee_tcb_svn2"))
                .mrServiceTd(readHex(tcbClaims, "tdx.quote.body.mr_servicetd"))
                .build();

        TdxQuoteClaims.TdAttributes tdAttributes = TdxQuoteClaims.TdAttributes.builder()
                .debug(readBoolean(tcbClaims, "tdx.td_attributes.debug"))
                .keyLocker(readBoolean(tcbClaims, "tdx.td_attributes.key_locker"))
                .perfmon(readBoolean(tcbClaims, "tdx.td_attributes.perfmon"))
                .protectionKeys(readBoolean(tcbClaims, "tdx.td_attributes.protection_keys"))
                .septveDisable(readBoolean(tcbClaims, "tdx.td_attributes.septve_disable"))
                .build();

        return TdxQuoteClaims.builder()
                .type(readHex(tcbClaims, "tdx.quote.type"))
                .size(readHex(tcbClaims, "tdx.quote.size"))
                .initData(readHex(tcbClaims, "init_data"))
                .reportData(readHex(tcbClaims, "report_data"))
                .header(header)
                .body(body)
                .tdAttributes(tdAttributes)
                .build();
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private String readString(Map<String, Object> claims, String path) {
        Object value = claims == null ? null : claims.get(path);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? null : text;
        }
        return String.valueOf(value);
    }

    private String readHex(Map<String, Object> claims, String path) {
        String value = readString(claims, path);
        return value == null ? null : HexUtils.normalize(value);
    }

    private Boolean readBoolean(Map<String, Object> claims, String path) {
        Object value = claims == null ? null : claims.get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String text = ((String) value).trim().toLowerCase();
            if ("true".equals(text) || "1".equals(text) || "yes".equals(text)) {
                return Boolean.TRUE;
            }
            if ("false".equals(text) || "0".equals(text) || "no".equals(text)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private JWK loadJwk(String keyId) {
        HttpRequest request = HttpRequest.newBuilder(config.getJwksUri())
                .timeout(config.getRequestTimeout())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("failed to load aliyun attestation jwks: http "
                        + response.statusCode() + ", body=" + trimForMessage(response.body()));
            }
            return JWKSet.parse(response.body()).getKeyByKeyId(keyId);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load aliyun attestation jwks: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("aliyun attestation jwks request was interrupted", ex);
        } catch (ParseException ex) {
            throw new IllegalStateException("failed to parse aliyun attestation jwks: " + ex.getMessage(), ex);
        }
    }

    private void validateRegisteredClaims(JWTClaimsSet claims) {
        if (!config.getIssuer().equals(claims.getIssuer())) {
            throw new IllegalStateException("unexpected aliyun attestation issuer: " + claims.getIssuer());
        }

        List<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(config.getAudience())) {
            throw new IllegalStateException("unexpected aliyun attestation audience: " + audience);
        }

        Instant now = clock.instant();
        Instant earliest = now.minus(config.getClockSkew());
        Instant latest = now.plus(config.getClockSkew());

        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null || expirationTime.toInstant().isBefore(earliest)) {
            throw new IllegalStateException("aliyun attestation jwt is expired");
        }

        Date notBeforeTime = claims.getNotBeforeTime();
        if (notBeforeTime == null || notBeforeTime.toInstant().isAfter(latest)) {
            throw new IllegalStateException("aliyun attestation jwt is not active yet");
        }

        Date issueTime = claims.getIssueTime();
        if (issueTime == null || issueTime.toInstant().isAfter(latest)) {
            throw new IllegalStateException("aliyun attestation jwt issue time is invalid");
        }
    }

    private QuoteEvidenceVerificationResult failure(String reason) {
        return QuoteEvidenceVerificationResult.builder()
                .valid(false)
                .provider(PROVIDER)
                .providerVersion(PROVIDER_VERSION)
                .failureReason(reason)
                .build();
    }

    private String policyIdsJson(List<String> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < policyIds.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(policyIds.get(i))).append('"');
        }
        return builder.append(']').toString();
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimForMessage(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
    }

    public static final class Builder {
        private AliyunRemoteAttestationConfig config = AliyunRemoteAttestationConfig.defaults();
        private HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getRequestTimeout())
                .build();
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder config(AliyunRemoteAttestationConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("config must not be null");
            }
            this.config = config;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            if (httpClient == null) {
                throw new IllegalArgumentException("httpClient must not be null");
            }
            this.httpClient = httpClient;
            return this;
        }

        public Builder clock(Clock clock) {
            if (clock == null) {
                throw new IllegalArgumentException("clock must not be null");
            }
            this.clock = clock;
            return this;
        }

        public AliyunRemoteQuoteEvidenceVerifier build() {
            return new AliyunRemoteQuoteEvidenceVerifier(this);
        }
    }
}
