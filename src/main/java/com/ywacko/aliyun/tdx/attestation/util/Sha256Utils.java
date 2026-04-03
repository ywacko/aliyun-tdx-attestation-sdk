package com.ywacko.aliyun.tdx.attestation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256Utils {

    private Sha256Utils() {
    }

    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static byte[] sha256Utf8(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(String input) {
        return HexUtils.toHex(sha256Utf8(input));
    }
}
