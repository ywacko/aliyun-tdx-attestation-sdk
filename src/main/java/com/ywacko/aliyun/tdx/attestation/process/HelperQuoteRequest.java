package com.ywacko.aliyun.tdx.attestation.process;

public class HelperQuoteRequest {

    private String reportDataHex;
    private String deploymentDigestHex;

    public HelperQuoteRequest() {
    }

    public HelperQuoteRequest(String reportDataHex, String deploymentDigestHex) {
        this.reportDataHex = reportDataHex;
        this.deploymentDigestHex = deploymentDigestHex;
    }

    public String getReportDataHex() {
        return reportDataHex;
    }

    public void setReportDataHex(String reportDataHex) {
        this.reportDataHex = reportDataHex;
    }

    public String getDeploymentDigestHex() {
        return deploymentDigestHex;
    }

    public void setDeploymentDigestHex(String deploymentDigestHex) {
        this.deploymentDigestHex = deploymentDigestHex;
    }
}
