package com.ywacko.aliyun.tdx.attestation.verify;

import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationRequest;
import com.ywacko.aliyun.tdx.attestation.verify.model.QuoteVerificationResult;

public interface QuoteVerifier {

    QuoteVerificationResult verify(QuoteVerificationRequest request);
}
