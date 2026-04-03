package com.ywacko.aliyun.tdx.attestation.model;

import com.ywacko.aliyun.tdx.attestation.util.HexUtils;
import com.ywacko.aliyun.tdx.attestation.util.Sha256Utils;

public final class DeploymentFingerprintReportDataFactory {

    private DeploymentFingerprintReportDataFactory() {
    }

    public static String canonicalJson(DeploymentFingerprint fingerprint) {
        return fingerprint.toCanonicalJson();
    }

    public static String digestHex(DeploymentFingerprint fingerprint) {
        return digestHex(fingerprint.toCanonicalJson());
    }

    public static String digestHex(String canonicalJson) {
        return Sha256Utils.sha256Hex(canonicalJson);
    }

    public static ReportData reportData(DeploymentFingerprint fingerprint) {
        return reportDataFromDigestHex(digestHex(fingerprint));
    }

    public static ReportData reportDataFromDigestHex(String digestHex) {
        byte[] digest = HexUtils.fromHex(digestHex);
        return ReportData.fromDeploymentDigest(digest);
    }
}
