package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProcessQuoteProviderTest {

    @Test
    void shouldInvokeHelperAndReturnQuote() throws Exception {
        Path tempDir = Files.createTempDirectory("aliyun-tdx-helper-test");
        Path helper = tempDir.resolve("mock-helper.sh");
        Files.writeString(helper, "#!/usr/bin/env sh\n"
                + "input=$(cat)\n"
                + "report_data=$(printf '%s' \"$input\" | sed -n 's/.*\"reportDataHex\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p')\n"
                + "printf '{\"quoteBase64\":\"cXVvdGUtYnl0ZXM=\",\"quoteSize\":11,\"reportDataHex\":\"%s\",\"provider\":\"mock-helper\",\"helperVersion\":\"test\"}' \"$report_data\"\n", StandardCharsets.UTF_8);
        helper.toFile().setExecutable(true);

        AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
                .helperCommand(List.of(helper.toString()), Duration.ofSeconds(5))
                .build();

        DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
                .service("tee-gateway")
                .containerName("tee-gateway")
                .imageRef("ywackoo/tee-gateway:20260402")
                .imageId("a6c80d35a995")
                .gitRev("021b2d7")
                .build();

        QuoteGenerationResult result = client.generateQuote(fingerprint);
        assertNotNull(result);
        assertEquals("cXVvdGUtYnl0ZXM=", result.getQuoteBase64());
        assertEquals("60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f581", result.getDeploymentDigestHex());
        assertEquals("mock-helper", result.getProvider());
        assertEquals("test", result.getHelperVersion());
        assertEquals(11, result.getQuoteSize());
        assertEquals("60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f5810000000000000000000000000000000000000000000000000000000000000000",
                result.getReportDataHex());
    }
}
