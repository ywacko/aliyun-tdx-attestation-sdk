package com.ywacko.aliyun.tdx.attestation.process;

public class HelperQuoteResponse {

    private String quoteBase64;
    private Integer quoteSize;
    private String reportDataHex;
    private String provider;
    private String helperVersion;

    public String getQuoteBase64() {
        return quoteBase64;
    }

    public void setQuoteBase64(String quoteBase64) {
        this.quoteBase64 = quoteBase64;
    }

    public Integer getQuoteSize() {
        return quoteSize;
    }

    public void setQuoteSize(Integer quoteSize) {
        this.quoteSize = quoteSize;
    }

    public String getReportDataHex() {
        return reportDataHex;
    }

    public void setReportDataHex(String reportDataHex) {
        this.reportDataHex = reportDataHex;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getHelperVersion() {
        return helperVersion;
    }

    public void setHelperVersion(String helperVersion) {
        this.helperVersion = helperVersion;
    }
}
