package com.ywacko.aliyun.tdx.attestation.jna;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.ywacko.aliyun.tdx.attestation.exception.QuoteGenerationException;

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
            return quotePointer.getByteArray(0, quoteSize);
        } finally {
            library.tdx_att_free_quote(quotePointer);
        }
    }
}
