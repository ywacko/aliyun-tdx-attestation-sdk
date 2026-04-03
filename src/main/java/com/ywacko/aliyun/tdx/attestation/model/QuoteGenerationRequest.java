package com.ywacko.aliyun.tdx.attestation.model;

public final class QuoteGenerationRequest {

    private final String deploymentDigestHex;
    private final ReportData reportData;

    private QuoteGenerationRequest(String deploymentDigestHex, ReportData reportData) {
        this.deploymentDigestHex = deploymentDigestHex;
        this.reportData = reportData;
    }

    public static QuoteGenerationRequest fromDeploymentFingerprint(DeploymentFingerprint fingerprint) {
        String canonicalJson = fingerprint.toCanonicalJson();
        String digestHex = DeploymentFingerprintReportDataFactory.digestHex(canonicalJson);
        ReportData reportData = DeploymentFingerprintReportDataFactory.reportDataFromDigestHex(digestHex);
        return new QuoteGenerationRequest(digestHex, reportData);
    }

    public static QuoteGenerationRequest fromDigestHexAndReportData(String deploymentDigestHex, ReportData reportData) {
        return new QuoteGenerationRequest(deploymentDigestHex, reportData);
    }

    public String getDeploymentDigestHex() {
        return deploymentDigestHex;
    }

    public ReportData getReportData() {
        return reportData;
    }
}
