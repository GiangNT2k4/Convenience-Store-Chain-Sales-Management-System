package com.chainstore.customer.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    /** Normalize VN phones to local form starting with 0 (e.g. 0912345678). */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        }
        digits = digits.replace("+", "");
        return digits;
    }

    public static boolean isValidVnMobile(String normalized) {
        return normalized != null && normalized.matches("^0\\d{9,10}$");
    }

    public static String syntheticEmail(String normalizedPhone) {
        return normalizedPhone + "@customer.chainstore.local";
    }
}
