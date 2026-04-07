package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprintReportDataFactory;
import com.ywacko.aliyun.tdx.attestation.model.ReportData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeploymentFingerprintReportDataFactoryTest {

    @Test
    void shouldBuildCanonicalJsonDigestAndReportData() {
        DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
                .service("tee-gateway")
                .imageDigest("ywackoo/tee-gateway@sha256:3872a935ba90b46925684a818401a682fb1aefd70b397e9c110bbbd2781aef46")
                .gitRev("021b2d7")
                .build();

        String canonicalJson = DeploymentFingerprintReportDataFactory.canonicalJson(fingerprint);
        assertEquals("{\"service\":\"tee-gateway\",\"image_digest\":\"ywackoo/tee-gateway@sha256:3872a935ba90b46925684a818401a682fb1aefd70b397e9c110bbbd2781aef46\",\"git_rev\":\"021b2d7\"}", canonicalJson);

        String digestHex = DeploymentFingerprintReportDataFactory.digestHex(fingerprint);
        assertEquals("cfed02c8b7159dda5478fa6df432c3626f18c5e457346d111dd9134192f7aa51", digestHex);

        ReportData reportData = DeploymentFingerprintReportDataFactory.reportData(fingerprint);
        assertEquals("cfed02c8b7159dda5478fa6df432c3626f18c5e457346d111dd9134192f7aa510000000000000000000000000000000000000000000000000000000000000000", reportData.toHex());
        assertArrayEquals(new byte[32], sliceTail(reportData.getBytes()));
    }

    private byte[] sliceTail(byte[] data) {
        byte[] tail = new byte[32];
        System.arraycopy(data, 32, tail, 0, 32);
        return tail;
    }
}
