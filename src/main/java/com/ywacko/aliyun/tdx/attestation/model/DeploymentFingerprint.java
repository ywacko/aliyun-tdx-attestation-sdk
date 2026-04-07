package com.ywacko.aliyun.tdx.attestation.model;

import java.util.Objects;

/**
 * 待绑定到 attestation 的部署级指纹。
 * 当前最终只把 service / image_digest / git_rev 纳入核心哈希。
 */
public final class DeploymentFingerprint {

    // 服务标识，当前固定对应 tee-gateway。
    private final String service;
    // 镜像内容标识，当前固定要求传入不可变 digest 口径。
    private final String imageDigest;
    // 代码版本，当前直接使用 git commit。
    private final String gitRev;

    private DeploymentFingerprint(Builder builder) {
        this.service = requireText(builder.service, "service");
        this.imageDigest = requireText(builder.imageDigest, "imageDigest");
        this.gitRev = requireText(builder.gitRev, "gitRev");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getService() {
        return service;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public String getGitRev() {
        return gitRev;
    }

    public String toCanonicalJson() {
        // 当前直接手工拼接固定字段顺序，避免序列化器差异影响摘要稳定性。
        return "{\"service\":\"" + escape(service)
                + "\",\"image_digest\":\"" + escape(imageDigest)
                + "\",\"git_rev\":\"" + escape(gitRev)
                + "\"}";
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentFingerprint)) {
            return false;
        }
        DeploymentFingerprint that = (DeploymentFingerprint) o;
        return Objects.equals(service, that.service)
                && Objects.equals(imageDigest, that.imageDigest)
                && Objects.equals(gitRev, that.gitRev);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, imageDigest, gitRev);
    }

    public static final class Builder {
        private String service;
        private String imageDigest;
        private String gitRev;

        private Builder() {
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder imageDigest(String imageDigest) {
            this.imageDigest = imageDigest;
            return this;
        }

        public Builder gitRev(String gitRev) {
            this.gitRev = gitRev;
            return this;
        }

        public DeploymentFingerprint build() {
            return new DeploymentFingerprint(this);
        }
    }
}
