package com.ywacko.aliyun.tdx.attestation.verify.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 阿里云远程证明 JWT 的注册字段与核心 EAT 字段。
 */
public final class AttestationTokenClaims {

    private final String keyId;
    private final String algorithm;
    private final String issuer;
    private final List<String> audience;
    private final Instant issuedAt;
    private final Instant notBefore;
    private final Instant expiresAt;
    private final String jwtId;
    private final String eatProfile;
    private final String intendedUse;
    private final String tee;
    private final String acsVersion;

    private AttestationTokenClaims(Builder builder) {
        this.keyId = builder.keyId;
        this.algorithm = builder.algorithm;
        this.issuer = builder.issuer;
        this.audience = immutableList(builder.audience);
        this.issuedAt = builder.issuedAt;
        this.notBefore = builder.notBefore;
        this.expiresAt = builder.expiresAt;
        this.jwtId = builder.jwtId;
        this.eatProfile = builder.eatProfile;
        this.intendedUse = builder.intendedUse;
        this.tee = builder.tee;
        this.acsVersion = builder.acsVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKeyId() {
        return keyId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getIssuer() {
        return issuer;
    }

    public List<String> getAudience() {
        return audience;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getJwtId() {
        return jwtId;
    }

    public String getEatProfile() {
        return eatProfile;
    }

    public String getIntendedUse() {
        return intendedUse;
    }

    public String getTee() {
        return tee;
    }

    public String getAcsVersion() {
        return acsVersion;
    }

    private static List<String> immutableList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static final class Builder {
        private String keyId;
        private String algorithm;
        private String issuer;
        private List<String> audience;
        private Instant issuedAt;
        private Instant notBefore;
        private Instant expiresAt;
        private String jwtId;
        private String eatProfile;
        private String intendedUse;
        private String tee;
        private String acsVersion;

        private Builder() {
        }

        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder audience(List<String> audience) {
            this.audience = audience;
            return this;
        }

        public Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder notBefore(Instant notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder jwtId(String jwtId) {
            this.jwtId = jwtId;
            return this;
        }

        public Builder eatProfile(String eatProfile) {
            this.eatProfile = eatProfile;
            return this;
        }

        public Builder intendedUse(String intendedUse) {
            this.intendedUse = intendedUse;
            return this;
        }

        public Builder tee(String tee) {
            this.tee = tee;
            return this;
        }

        public Builder acsVersion(String acsVersion) {
            this.acsVersion = acsVersion;
            return this;
        }

        public AttestationTokenClaims build() {
            return new AttestationTokenClaims(this);
        }
    }
}
