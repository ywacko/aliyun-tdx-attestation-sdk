package com.ywacko.aliyun.tdx.attestation.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

/**
 * libtdx_attest.so 的最小 JNA 映射。
 * 当前只映射 Quote 生成链路所需的接口和结构体。
 */
public interface TdxAttestLibrary extends Library {

    int TDX_REPORT_DATA_SIZE = 64;

    static TdxAttestLibrary load(String libraryName) {
        return Native.load(libraryName, TdxAttestLibrary.class);
    }

    int tdx_att_get_quote(TdxReportData reportData,
                          Pointer attKeyIdList,
                          int listSize,
                          Pointer attKeyId,
                          PointerByReference quoteBuffer,
                          IntByReference quoteSize,
                          int flags);

    void tdx_att_free_quote(Pointer quoteBuffer);

    class TdxReportData extends Structure {
        // tdx_report_data_t 的固定 64 字节载体。
        public byte[] d = new byte[TDX_REPORT_DATA_SIZE];

        public TdxReportData() {
        }

        public TdxReportData(byte[] bytes) {
            if (bytes.length != TDX_REPORT_DATA_SIZE) {
                throw new IllegalArgumentException("report_data must be exactly 64 bytes");
            }
            System.arraycopy(bytes, 0, d, 0, TDX_REPORT_DATA_SIZE);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("d");
        }
    }
}
