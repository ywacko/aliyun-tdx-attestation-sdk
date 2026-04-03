package com.ywacko.aliyun.tdx.attestation.exception;

public class AttestationException extends RuntimeException {

    public AttestationException(String message) {
        super(message);
    }

    public AttestationException(String message, Throwable cause) {
        super(message, cause);
    }
}
