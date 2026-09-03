package com.chainstore.customer.util;

import java.util.regex.Pattern;

public final class EmailNormalizer {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private EmailNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().toLowerCase();
    }

    public static boolean isValid(String normalized) {
        return normalized != null
                && !normalized.endsWith("@customer.chainstore.local")
                && EMAIL.matcher(normalized).matches();
    }
}
