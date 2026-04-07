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
                .imageDigest("ywackoo/tee-gateway@sha256:3872a935ba90b46925684a818401a682fb1aefd70b397e9c110bbbd2781aef46")
                .gitRev("021b2d7")
                .build();

        QuoteGenerationResult result = provider.generateQuote(
                com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest.fromDeploymentFingerprint(fingerprint)
        );

        assertNotNull(result);
        assertArrayEquals(fakeQuote, result.getQuoteBytes());
        assertEquals("aliyun-tdx-jna", result.getProvider());
        assertEquals(Integer.valueOf(fakeQuote.length), result.getQuoteSize());
        assertEquals("cfed02c8b7159dda5478fa6df432c3626f18c5e457346d111dd9134192f7aa51", result.getDeploymentDigestHex());
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
