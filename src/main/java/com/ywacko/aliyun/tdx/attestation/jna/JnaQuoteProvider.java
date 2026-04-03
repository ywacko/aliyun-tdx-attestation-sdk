package com.ywacko.aliyun.tdx.attestation.jna;

import com.ywacko.aliyun.tdx.attestation.exception.QuoteGenerationException;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 通过 JNA 直接生成 Quote 的 provider。
 * 当前要求调用进程直接运行在阿里云 TDX VM 中。
 */
public final class JnaQuoteProvider {

    // 实际执行 native 调用的封装层。
    private final NativeTdxAttestationApi nativeApi;
    // 当前固定检查的 TDX 设备路径。
    private final Path tdxDevicePath;

    private JnaQuoteProvider(Builder builder) {
        this.nativeApi = Objects.requireNonNull(builder.nativeApi, "nativeApi");
        this.tdxDevicePath = builder.tdxDevicePath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public QuoteGenerationResult generateQuote(QuoteGenerationRequest request) {
        Objects.requireNonNull(request, "request");
        validateEnvironment();

        byte[] quoteBytes = nativeApi.getQuote(request.getReportData().getBytes());
        return new QuoteGenerationResult(
                quoteBytes,
                request.getDeploymentDigestHex(),
                request.getReportData().toHex(),
                quoteBytes.length,
                "aliyun-tdx-jna",
                null
        );
    }

    private void validateEnvironment() {
        if (!Files.exists(tdxDevicePath)) {
            throw new QuoteGenerationException("TDX device not found: " + tdxDevicePath
                    + ". This SDK must run on an Aliyun TDX VM with /dev/tdx_guest available.");
        }
    }

    public static final class Builder {
        // 延迟到 build 阶段再加载动态库，避免普通开发机在构造时直接失败。
        private NativeTdxAttestationApi nativeApi;
        // 默认直接加载系统中的 libtdx_attest.so。
        private String libraryName = "tdx_attest";
        // 默认设备路径与当前阿里云 TDX VM 环境保持一致。
        private Path tdxDevicePath = Path.of("/dev/tdx_guest");

        private Builder() {
        }

        public Builder nativeApi(NativeTdxAttestationApi nativeApi) {
            this.nativeApi = nativeApi;
            return this;
        }

        public Builder libraryName(String libraryName) {
            this.libraryName = libraryName;
            return this;
        }

        public Builder tdxDevicePath(Path tdxDevicePath) {
            this.tdxDevicePath = tdxDevicePath;
            return this;
        }

        public JnaQuoteProvider build() {
            if (nativeApi == null) {
                nativeApi = new NativeTdxAttestationApi(TdxAttestLibrary.load(libraryName));
            }
            return new JnaQuoteProvider(this);
        }
    }
}
