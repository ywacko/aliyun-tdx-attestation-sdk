package com.ywacko.aliyun.tdx.attestation.verify;

/**
 * Quote 远程证明验证扩展点。
 * 默认实现为 AliyunRemoteQuoteEvidenceVerifier；该接口保留给私有 verifier 或测试替换。
 */
public interface QuoteEvidenceVerifier {

    QuoteEvidenceVerificationResult verify(byte[] quoteBytes, String expectedReportDataHex);
}
