package com.ywacko.aliyun.tdx.attestation.model;

import java.util.Objects;

/**
 * 待绑定到 attestation 的部署级指纹。
 * 当前字段顺序固定，用于生成稳定 canonical JSON。
 */
public final class DeploymentFingerprint {

    // 服务标识，当前固定对应 tee-gateway。
    private final String service;
    // 容器名，用于表达当前运行实例的部署口径。
    private final String containerName;
    // 镜像引用，通常包含仓库名和 tag。
    private final String imageRef;
    // 镜像内容标识，优先使用 image id 或 digest。
    private final String imageId;
    // 代码版本，当前直接使用 git commit。
    private final String gitRev;

    private DeploymentFingerprint(Builder builder) {
        this.service = requireText(builder.service, "service");
        this.containerName = requireText(builder.containerName, "containerName");
        this.imageRef = requireText(builder.imageRef, "imageRef");
        this.imageId = requireText(builder.imageId, "imageId");
        this.gitRev = requireText(builder.gitRev, "gitRev");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getService() {
        return service;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getImageRef() {
        return imageRef;
    }

    public String getImageId() {
        return imageId;
    }

    public String getGitRev() {
        return gitRev;
    }

    public String toCanonicalJson() {
        // 当前直接手工拼接固定字段顺序，避免序列化器差异影响摘要稳定性。
        return "{\"service\":\"" + escape(service)
                + "\",\"container_name\":\"" + escape(containerName)
                + "\",\"image_ref\":\"" + escape(imageRef)
                + "\",\"image_id\":\"" + escape(imageId)
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
                && Objects.equals(containerName, that.containerName)
                && Objects.equals(imageRef, that.imageRef)
                && Objects.equals(imageId, that.imageId)
                && Objects.equals(gitRev, that.gitRev);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, containerName, imageRef, imageId, gitRev);
    }

    public static final class Builder {
        private String service;
        private String containerName;
        private String imageRef;
        private String imageId;
        private String gitRev;

        private Builder() {
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder containerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder imageRef(String imageRef) {
            this.imageRef = imageRef;
            return this;
        }

        public Builder imageId(String imageId) {
            this.imageId = imageId;
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
