package com.ywacko.aliyun.tdx.attestation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.jna.NativeTdxAttestationApi;
import com.ywacko.aliyun.tdx.attestation.jna.TdxAttestLibrary;
import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JnaQuoteProviderTest {

    @Test
    void shouldInvokeNativeApiThroughJnaProvider() throws Exception {
        byte[] fakeQuote = "quote-bytes".getBytes();
        FakeTdxAttestLibrary fakeLibrary = new FakeTdxAttestLibrary(fakeQuote);
        NativeTdxAttestationApi nativeApi = new NativeTdxAttestationApi(fakeLibrary);

        JnaQuoteProvider provider = JnaQuoteProvider.builder()
                .nativeApi(nativeApi)
                .tdxDevicePath(Files.createTempFile("tdx-device", ".mock"))
                .build();

        DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
                .service("tee-gateway")
                .containerName("tee-gateway")
                .imageRef("ywackoo/tee-gateway:20260402")
                .imageId("a6c80d35a995")
                .gitRev("021b2d7")
                .build();

        QuoteGenerationResult result = provider.generateQuote(
                com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest.fromDeploymentFingerprint(fingerprint)
        );

        assertNotNull(result);
        assertArrayEquals(fakeQuote, result.getQuoteBytes());
        assertEquals("aliyun-tdx-jna", result.getProvider());
        assertEquals(Integer.valueOf(fakeQuote.length), result.getQuoteSize());
        assertEquals("60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f581", result.getDeploymentDigestHex());
    }

    private static final class FakeTdxAttestLibrary implements TdxAttestLibrary {
        private final byte[] quoteBytes;

        private FakeTdxAttestLibrary(byte[] quoteBytes) {
            this.quoteBytes = quoteBytes.clone();
        }

        @Override
        public int tdx_att_get_quote(TdxReportData reportData,
                                     Pointer attKeyIdList,
                                     int listSize,
                                     Pointer attKeyId,
                                     PointerByReference quoteBuffer,
                                     IntByReference quoteSize,
                                     int flags) {
            Memory memory = new Memory(quoteBytes.length);
            memory.write(0, quoteBytes, 0, quoteBytes.length);
            quoteBuffer.setValue(memory);
            quoteSize.setValue(quoteBytes.length);
            return 0;
        }

        @Override
        public void tdx_att_free_quote(Pointer quoteBuffer) {
            // no-op for the in-memory fake implementation
        }
    }
}
