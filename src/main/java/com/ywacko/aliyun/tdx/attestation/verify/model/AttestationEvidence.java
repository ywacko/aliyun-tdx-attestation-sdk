package com.ywacko.aliyun.tdx.attestation.verify.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 远程证明服务返回并经 SDK 验签后的证明载荷。
 */
public final class AttestationEvidence {

    private final String rawJwt;
    private final AttestationTokenClaims tokenClaims;
    private final TdxQuoteClaims tdxQuote;
    private final Map<String, Object> rawClaims;
    private final Map<String, Object> tcbStatusClaims;
    private final Object evaluationReports;
    private final Object customizedClaims;

    private AttestationEvidence(Builder builder) {
        this.rawJwt = builder.rawJwt;
        this.tokenClaims = builder.tokenClaims;
        this.tdxQuote = builder.tdxQuote;
        this.rawClaims = immutableMap(builder.rawClaims);
        this.tcbStatusClaims = immutableMap(builder.tcbStatusClaims);
        this.evaluationReports = builder.evaluationReports;
        this.customizedClaims = builder.customizedClaims;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRawJwt() {
        return rawJwt;
    }

    public AttestationTokenClaims getTokenClaims() {
        return tokenClaims;
    }

    public TdxQuoteClaims getTdxQuote() {
        return tdxQuote;
    }

    public Map<String, Object> getRawClaims() {
        return rawClaims;
    }

    public Map<String, Object> getTcbStatusClaims() {
        return tcbStatusClaims;
    }

    public Object getEvaluationReports() {
        return evaluationReports;
    }

    public Object getCustomizedClaims() {
        return customizedClaims;
    }

    public String getAttestedReportDataHex() {
        if (tdxQuote == null || tdxQuote.getBody() == null) {
            return null;
        }
        return tdxQuote.getBody().getReportData();
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static final class Builder {
        private String rawJwt;
        private AttestationTokenClaims tokenClaims;
        private TdxQuoteClaims tdxQuote;
        private Map<String, Object> rawClaims;
        private Map<String, Object> tcbStatusClaims;
        private Object evaluationReports;
        private Object customizedClaims;

        private Builder() {
        }

        public Builder rawJwt(String rawJwt) {
            this.rawJwt = rawJwt;
            return this;
        }

        public Builder tokenClaims(AttestationTokenClaims tokenClaims) {
            this.tokenClaims = tokenClaims;
            return this;
        }

        public Builder tdxQuote(TdxQuoteClaims tdxQuote) {
            this.tdxQuote = tdxQuote;
            return this;
        }

        public Builder rawClaims(Map<String, Object> rawClaims) {
            this.rawClaims = rawClaims;
            return this;
        }

        public Builder tcbStatusClaims(Map<String, Object> tcbStatusClaims) {
            this.tcbStatusClaims = tcbStatusClaims;
            return this;
        }

        public Builder evaluationReports(Object evaluationReports) {
            this.evaluationReports = evaluationReports;
            return this;
        }

        public Builder customizedClaims(Object customizedClaims) {
            this.customizedClaims = customizedClaims;
            return this;
        }

        public AttestationEvidence build() {
            return new AttestationEvidence(this);
        }
    }
}
