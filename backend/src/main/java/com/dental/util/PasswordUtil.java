package com.dental.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** One-way password hashing (SHA-256, hex encoded). */
public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String plain) {
        if (plain == null) {
            plain = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Constant-time-ish comparison of a plaintext attempt against a stored digest. */
    public static boolean matches(String plain, String storedHash) {
        if (storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(plain).getBytes(StandardCharsets.UTF_8),
                storedHash.trim().getBytes(StandardCharsets.UTF_8));
    }
}
