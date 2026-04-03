package com.ywacko.aliyun.tdx.attestation.model;

import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;

import java.util.Base64;
import java.util.Objects;

/**
 * Quote 生成结果。
 * 当前同时保留原始字节、Base64 结果和 report_data 元信息。
 */
public final class QuoteGenerationResult {

    // 原始 Quote 字节，便于后续直接持久化或继续验证。
    private final byte[] quoteBytes;
    // 原始部署级指纹摘要。
    private final String deploymentDigestHex;
    // 实际写入 native 调用的 64 字节 report_data。
    private final String reportDataHex;
    // Quote 原始长度。
    private final Integer quoteSize;
    // 生成提供方标识，当前主链路为 aliyun-tdx-jna。
    private final String provider;
    // 提供方版本，当前可按实现逐步补齐。
    private final String providerVersion;

    public QuoteGenerationResult(byte[] quoteBytes,
                                 String deploymentDigestHex,
                                 String reportDataHex,
                                 Integer quoteSize,
                                 String provider,
                                 String providerVersion) {
        this.quoteBytes = Objects.requireNonNull(quoteBytes, "quoteBytes").clone();
        this.deploymentDigestHex = Objects.requireNonNull(deploymentDigestHex, "deploymentDigestHex");
        this.reportDataHex = Objects.requireNonNull(reportDataHex, "reportDataHex");
        this.quoteSize = quoteSize;
        this.provider = provider;
        this.providerVersion = providerVersion;
    }

    public byte[] getQuoteBytes() {
        return quoteBytes.clone();
    }

    public String getQuoteBase64() {
        return Base64.getEncoder().encodeToString(quoteBytes);
    }

    public String getQuoteSha256Hex() {
        return HexUtils.toHex(Sha256Utils.sha256(quoteBytes));
    }

    public String getDeploymentDigestHex() {
        return deploymentDigestHex;
    }

    public String getReportDataHex() {
        return reportDataHex;
    }

    public Integer getQuoteSize() {
        return quoteSize;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }
}
