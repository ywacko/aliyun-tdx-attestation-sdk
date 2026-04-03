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
                .containerName("tee-gateway")
                .imageRef("ywackoo/tee-gateway:20260402")
                .imageId("a6c80d35a995")
                .gitRev("021b2d7")
                .build();

        String canonicalJson = DeploymentFingerprintReportDataFactory.canonicalJson(fingerprint);
        assertEquals("{\"service\":\"tee-gateway\",\"container_name\":\"tee-gateway\",\"image_ref\":\"ywackoo/tee-gateway:20260402\",\"image_id\":\"a6c80d35a995\",\"git_rev\":\"021b2d7\"}", canonicalJson);

        String digestHex = DeploymentFingerprintReportDataFactory.digestHex(fingerprint);
        assertEquals("60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f581", digestHex);

        ReportData reportData = DeploymentFingerprintReportDataFactory.reportData(fingerprint);
        assertEquals("60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f5810000000000000000000000000000000000000000000000000000000000000000", reportData.toHex());
        assertArrayEquals(new byte[32], sliceTail(reportData.getBytes()));
    }

    private byte[] sliceTail(byte[] data) {
        byte[] tail = new byte[32];
        System.arraycopy(data, 32, tail, 0, 32);
        return tail;
    }
}
