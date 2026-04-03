package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.jna.NativeTdxAttestationApi;

import java.nio.file.Path;

/**
 * 阿里云 TDX 远程证明客户端。
 * 当前仅保留通过 JNA 调本机 libtdx_attest.so 的单链路实现。
 */
public final class AliyunTdxAttestationClient {

    // 当前固定通过 JNA provider 生成 Quote。
    private final JnaQuoteProvider quoteProvider;

    private AliyunTdxAttestationClient(Builder builder) {
        this.quoteProvider = builder.jnaBuilder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public QuoteGenerationResult generateQuote(DeploymentFingerprint fingerprint) {
        return generateQuote(QuoteGenerationRequest.fromDeploymentFingerprint(fingerprint));
    }

    public QuoteGenerationResult generateQuote(QuoteGenerationRequest request) {
        return quoteProvider.generateQuote(request);
    }

    public static final class Builder {
        // 当前只保留 JNA builder，便于按需覆写库名和设备路径。
        private final JnaQuoteProvider.Builder jnaBuilder = JnaQuoteProvider.builder();

        private Builder() {
        }

        // 允许测试或特殊环境显式替换 native API。
        public Builder nativeApi(NativeTdxAttestationApi nativeApi) {
            jnaBuilder.nativeApi(nativeApi);
            return this;
        }

        public Builder libraryName(String libraryName) {
            jnaBuilder.libraryName(libraryName);
            return this;
        }

        public Builder tdxDevicePath(Path tdxDevicePath) {
            jnaBuilder.tdxDevicePath(tdxDevicePath);
            return this;
        }

        public AliyunTdxAttestationClient build() {
            return new AliyunTdxAttestationClient(this);
        }
    }
}
