package com.ywacko.aliyun.tdx.attestation;

import com.ywacko.aliyun.tdx.attestation.model.DeploymentFingerprint;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;
import com.ywacko.aliyun.tdx.attestation.jna.JnaQuoteProvider;
import com.ywacko.aliyun.tdx.attestation.jna.NativeTdxAttestationApi;
import com.ywacko.aliyun.tdx.attestation.verify.AliyunRemoteAttestationConfig;
import com.ywacko.aliyun.tdx.attestation.verify.AliyunRemoteQuoteEvidenceVerifier;
import com.ywacko.aliyun.tdx.attestation.verify.DefaultQuoteVerifier;
import com.ywacko.aliyun.tdx.attestation.verify.QuoteEvidenceVerifier;
import com.ywacko.aliyun.tdx.attestation.model.TdxEnvironmentProfile;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationDetails;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationRequest;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;

import java.time.Instant;
import java.nio.file.Path;

/**
 * 阿里云 TDX 远程证明客户端。
 * 当前仅保留通过 JNA 调本机 libtdx_attest.so 的单链路实现。
 */
public final class AliyunTdxAttestationClient {

    // 当前固定通过 JNA provider 生成 Quote；懒加载避免只做验证时也加载 native 库。
    private final JnaQuoteProvider.Builder jnaBuilder;
    private volatile JnaQuoteProvider quoteProvider;
    // 当前验证接口默认做字段自洽校验，并通过可插拔 verifier 承接证明服务结果。
    private final DefaultQuoteVerifier quoteVerifier;

    private AliyunTdxAttestationClient(Builder builder) {
        this.jnaBuilder = builder.jnaBuilder;
        this.quoteVerifier = builder.verifierBuilder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public QuoteGenerationResult generateQuote(DeploymentFingerprint fingerprint) {
        return generateQuote(QuoteGenerationRequest.fromDeploymentFingerprint(fingerprint));
    }

    public QuoteGenerationResult generateQuote(QuoteGenerationRequest request) {
        return quoteProvider().generateQuote(request);
    }

    public QuoteVerificationResult verifyQuote(QuoteVerificationRequest request) {
        return quoteVerifier.verify(request);
    }

    public TdxEnvironmentProfile attestCurrentEnvironment(DeploymentFingerprint fingerprint) {
        QuoteGenerationResult quote = generateQuote(fingerprint);
        QuoteVerificationRequest request = QuoteVerificationRequest.builder()
                .service(fingerprint.getService())
                .imageDigest(fingerprint.getImageDigest())
                .gitRev(fingerprint.getGitRev())
                .deploymentDigestHex(quote.getDeploymentDigestHex())
                .reportDataHex(quote.getReportDataHex())
                .quoteBase64(quote.getQuoteBase64())
                .quoteSha256Hex(quote.getQuoteSha256Hex())
                .quoteSize(quote.getQuoteSize())
                .provider(quote.getProvider())
                .providerVersion(quote.getProviderVersion())
                .build();
        QuoteVerificationDetails details = quoteVerifier.verifyDetailed(request);
        return new TdxEnvironmentProfile(fingerprint, quote, details.getVerification(), details.getEvidence(), Instant.now());
    }

    private JnaQuoteProvider quoteProvider() {
        JnaQuoteProvider local = quoteProvider;
        if (local == null) {
            synchronized (this) {
                local = quoteProvider;
                if (local == null) {
                    local = jnaBuilder.build();
                    quoteProvider = local;
                }
            }
        }
        return local;
    }

    public static final class Builder {
        // 当前只保留 JNA builder，便于按需覆写库名和设备路径。
        private final JnaQuoteProvider.Builder jnaBuilder = JnaQuoteProvider.builder();
        private final DefaultQuoteVerifier.Builder verifierBuilder = DefaultQuoteVerifier.builder();

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

        public Builder quoteEvidenceVerifier(QuoteEvidenceVerifier quoteEvidenceVerifier) {
            verifierBuilder.evidenceVerifier(quoteEvidenceVerifier);
            return this;
        }

        public Builder remoteAttestationConfig(AliyunRemoteAttestationConfig config) {
            verifierBuilder.evidenceVerifier(AliyunRemoteQuoteEvidenceVerifier.builder()
                    .config(config)
                    .build());
            return this;
        }

        public AliyunTdxAttestationClient build() {
            return new AliyunTdxAttestationClient(this);
        }
    }
}
