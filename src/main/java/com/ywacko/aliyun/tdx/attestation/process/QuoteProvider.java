package com.ywacko.aliyun.tdx.attestation.process;

import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationRequest;
import com.ywacko.aliyun.tdx.attestation.model.QuoteGenerationResult;

public interface QuoteProvider {

    QuoteGenerationResult generateQuote(QuoteGenerationRequest request);
}
