package com.ywacko.aliyun.tdx.attestation.jna;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.ywacko.aliyun.tdx.attestation.exception.QuoteGenerationException;

/**
 * 对 libtdx_attest.so 的最小 native 调用封装。
 * 当前只负责把 64 字节 report_data 换成 Quote 字节。
 */
public class NativeTdxAttestationApi {

    private final TdxAttestLibrary library;

    public NativeTdxAttestationApi(TdxAttestLibrary library) {
        this.library = library;
    }

    public byte[] getQuote(byte[] reportDataBytes) {
        TdxAttestLibrary.TdxReportData reportData = new TdxAttestLibrary.TdxReportData(reportDataBytes);
        PointerByReference quoteBufferRef = new PointerByReference();
        IntByReference quoteSizeRef = new IntByReference();

        int result = library.tdx_att_get_quote(reportData, Pointer.NULL, 0, Pointer.NULL, quoteBufferRef, quoteSizeRef, 0);
        if (result != 0) {
            throw new QuoteGenerationException(String.format("tdx_att_get_quote failed: 0x%04x", result));
        }

        Pointer quotePointer = quoteBufferRef.getValue();
        if (quotePointer == null) {
            throw new QuoteGenerationException("native library returned a null quote pointer");
        }

        int quoteSize = quoteSizeRef.getValue();
        if (quoteSize <= 0) {
            library.tdx_att_free_quote(quotePointer);
            throw new QuoteGenerationException("native library returned an invalid quote size: " + quoteSize);
        }

        try {
            // 当前直接把 native 返回的连续内存拷成 Java byte[]。
            return quotePointer.getByteArray(0, quoteSize);
        } finally {
            // Quote 内存由 libtdx_attest 分配，释放也必须走对应 native 接口。
            library.tdx_att_free_quote(quotePointer);
        }
    }
}
