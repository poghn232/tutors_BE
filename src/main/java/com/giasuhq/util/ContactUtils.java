package com.giasuhq.util;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class ContactUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("0[35789]\\d{8}");

    private ContactUtils() {
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    public static String normalizePhone(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().replaceAll("[\\s().-]", "");
        if (normalized.startsWith("+84")) {
            return "0" + normalized.substring(3);
        }
        if (normalized.startsWith("84") && normalized.length() == 11) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }

    public static boolean isValidPhone(String value) {
        String normalized = normalizePhone(value);
        return !StringUtils.hasText(normalized) || PHONE_PATTERN.matcher(normalized).matches();
    }
}
