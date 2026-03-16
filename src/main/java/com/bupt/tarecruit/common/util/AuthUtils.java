package com.bupt.tarecruit.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class AuthUtils {
    private AuthUtils() {
    }

    public static String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public static String generateSalt() {
        return UUID.randomUUID().toString();
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public static String hashPassword(String password, String salt) {
        return sha256(salt + ":" + password);
    }
}
