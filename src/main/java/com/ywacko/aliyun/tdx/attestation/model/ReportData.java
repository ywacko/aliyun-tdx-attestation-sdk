package com.ywacko.aliyun.tdx.attestation.model;

import com.ywacko.aliyun.tdx.attestation.util.HexUtils;

import java.util.Arrays;

public final class ReportData {

    public static final int REPORT_DATA_SIZE = 64;
    public static final int DEPLOYMENT_DIGEST_SIZE = 32;

    private final byte[] bytes;

    private ReportData(byte[] bytes) {
        if (bytes.length != REPORT_DATA_SIZE) {
            throw new IllegalArgumentException("report_data must be exactly 64 bytes");
        }
        this.bytes = bytes.clone();
    }

    public static ReportData fromBytes(byte[] bytes) {
        return new ReportData(bytes);
    }

    public static ReportData fromDeploymentDigest(byte[] digest) {
        if (digest.length != DEPLOYMENT_DIGEST_SIZE) {
            throw new IllegalArgumentException("deployment digest must be exactly 32 bytes");
        }
        byte[] reportData = new byte[REPORT_DATA_SIZE];
        System.arraycopy(digest, 0, reportData, 0, DEPLOYMENT_DIGEST_SIZE);
        return new ReportData(reportData);
    }

    public byte[] getBytes() {
        return bytes.clone();
    }

    public String toHex() {
        return HexUtils.toHex(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportData)) {
            return false;
        }
        ReportData that = (ReportData) o;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
