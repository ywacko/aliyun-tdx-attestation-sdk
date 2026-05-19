package com.ywacko.aliyun.tdx.attestation.verify;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 阿里云官方远程证明服务配置。
 */
public final class AliyunRemoteAttestationConfig {

    public static final URI DEFAULT_ATTESTATION_ENDPOINT =
            URI.create("https://attest.cn-beijing.aliyuncs.com/v1/attestation");
    public static final URI DEFAULT_JWKS_URI =
            URI.create("https://attest.cn-beijing.aliyuncs.com/jwks.json");
    public static final String DEFAULT_ISSUER = "https://attest.cn-beijing.aliyuncs.com";
    public static final String DEFAULT_AUDIENCE = "https://attest.cn-beijing.aliyuncs.com";

    private final URI attestationEndpoint;
    private final URI jwksUri;
    private final String issuer;
    private final String audience;
    private final List<String> policyIds;
    private final Duration requestTimeout;
    private final Duration clockSkew;

    private AliyunRemoteAttestationConfig(Builder builder) {
        this.attestationEndpoint = builder.attestationEndpoint;
        this.jwksUri = builder.jwksUri;
        this.issuer = builder.issuer;
        this.audience = builder.audience;
        this.policyIds = Collections.unmodifiableList(new ArrayList<>(builder.policyIds));
        this.requestTimeout = builder.requestTimeout;
        this.clockSkew = builder.clockSkew;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AliyunRemoteAttestationConfig defaults() {
        return builder().build();
    }

    public URI getAttestationEndpoint() {
        return attestationEndpoint;
    }

    public URI getJwksUri() {
        return jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public List<String> getPolicyIds() {
        return policyIds;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public static final class Builder {
        private URI attestationEndpoint = DEFAULT_ATTESTATION_ENDPOINT;
        private URI jwksUri = DEFAULT_JWKS_URI;
        private String issuer = DEFAULT_ISSUER;
        private String audience = DEFAULT_AUDIENCE;
        private List<String> policyIds = Collections.emptyList();
        private Duration requestTimeout = Duration.ofSeconds(10);
        private Duration clockSkew = Duration.ofMinutes(2);

        private Builder() {
        }

        public Builder attestationEndpoint(URI attestationEndpoint) {
            this.attestationEndpoint = requireNonNull(attestationEndpoint, "attestationEndpoint");
            return this;
        }

        public Builder jwksUri(URI jwksUri) {
            this.jwksUri = requireNonNull(jwksUri, "jwksUri");
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = requireText(issuer, "issuer");
            return this;
        }

        public Builder audience(String audience) {
            this.audience = requireText(audience, "audience");
            return this;
        }

        public Builder policyIds(List<String> policyIds) {
            if (policyIds == null) {
                this.policyIds = Collections.emptyList();
            } else {
                this.policyIds = new ArrayList<>(policyIds);
            }
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
            return this;
        }

        public Builder clockSkew(Duration clockSkew) {
            if (clockSkew == null || clockSkew.isNegative()) {
                throw new IllegalArgumentException("clockSkew must not be negative");
            }
            this.clockSkew = clockSkew;
            return this;
        }

        public AliyunRemoteAttestationConfig build() {
            return new AliyunRemoteAttestationConfig(this);
        }

        private static <T> T requireNonNull(T value, String name) {
            if (value == null) {
                throw new IllegalArgumentException(name + " must not be null");
            }
            return value;
        }

        private static String requireText(String value, String name) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }

        private static Duration requirePositive(Duration value, String name) {
            if (value == null || value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
