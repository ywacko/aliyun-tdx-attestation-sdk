package com.ywacko.aliyun.tdx.attestation.model;

import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;

import java.util.Base64;
import java.util.Objects;

public final class QuoteGenerationResult {

    private final byte[] quoteBytes;
    private final String deploymentDigestHex;
    private final String reportDataHex;
    private final Integer quoteSize;
    private final String provider;
    private final String helperVersion;

    public QuoteGenerationResult(byte[] quoteBytes,
                                 String deploymentDigestHex,
                                 String reportDataHex,
                                 Integer quoteSize,
                                 String provider,
                                 String helperVersion) {
        this.quoteBytes = Objects.requireNonNull(quoteBytes, "quoteBytes").clone();
        this.deploymentDigestHex = Objects.requireNonNull(deploymentDigestHex, "deploymentDigestHex");
        this.reportDataHex = Objects.requireNonNull(reportDataHex, "reportDataHex");
        this.quoteSize = quoteSize;
        this.provider = provider;
        this.helperVersion = helperVersion;
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

    public String getHelperVersion() {
        return helperVersion;
    }
}
