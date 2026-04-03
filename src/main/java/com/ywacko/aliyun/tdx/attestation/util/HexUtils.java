package com.ywacko.aliyun.tdx.attestation.util;

public final class HexUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexUtils() {
    }

    public static String toHex(byte[] data) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xff;
            out[i * 2] = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0f];
        }
        return new String(out);
    }

    public static byte[] fromHex(String value) {
        String hex = normalize(value);
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("hex length must be even");
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex string");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("hex value must not be null");
        }
        String normalized = value.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        return normalized.toLowerCase();
    }
}
