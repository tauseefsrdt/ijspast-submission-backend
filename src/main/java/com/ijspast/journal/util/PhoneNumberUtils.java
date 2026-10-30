package com.ijspast.journal.util;

import java.util.regex.Pattern;

/**
 * Enterprise Phone Number Normalization and Matching Utility
 *
 * Provides robust E.164-compatible canonicalization, national significant number
 * extraction, and cross-format duplicate detection across international country
 * codes and formatting styles (spaces, hyphens, parentheses, leading zeros, and trunk prefixes).
 */
public final class PhoneNumberUtils {

    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^0-9]");
    private static final String DEFAULT_COUNTRY_CODE = "+91"; // India default for IJSPAST / SRMU

    private PhoneNumberUtils() {
    }

    /**
     * Normalizes a raw phone string into a canonical international E.164-compatible string.
     * Returns null if raw is null or blank.
     *
     * Examples:
     *  "+91 98765-43210"   -> "+919876543210"
     *  "09876543210"       -> "+919876543210"
     *  "9876543210"        -> "+919876543210"
     *  "+1 (555) 123-4567" -> "+15551234567"
     *  "1-555-123-4567"    -> "+15551234567"
     *  "0044 20 7946 0958" -> "+442079460958"
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Handle leading 00 (international call prefix) -> replace with +
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2).trim();
        }

        boolean hasPlus = trimmed.startsWith("+");
        String digitsOnly = NON_DIGIT_PATTERN.matcher(trimmed).replaceAll("");

        if (digitsOnly.isEmpty()) {
            return null;
        }

        if (hasPlus) {
            return "+" + digitsOnly;
        }

        // If no leading '+' was provided:
        // Case A: 11 digits starting with 1 (US / North America)
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            return "+" + digitsOnly;
        }

        // Case B: 11 digits starting with 0 (Indian / UK national trunk zero)
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("0")) {
            String national = digitsOnly.substring(1);
            if (national.length() == 10 && (national.charAt(0) >= '6' && national.charAt(0) <= '9')) {
                return DEFAULT_COUNTRY_CODE + national;
            }
            return DEFAULT_COUNTRY_CODE + national;
        }

        // Case C: Standard 10-digit mobile number
        if (digitsOnly.length() == 10) {
            // In the SRMU / IJSPAST context, 10-digit mobile numbers default to India (+91)
            return DEFAULT_COUNTRY_CODE + digitsOnly;
        }

        // Case D: Any other international number provided without '+' (e.g. 919876543210)
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }

        return "+" + digitsOnly;
    }

    /**
     * Extracts the core national significant digits (last 10 digits for numbers with >= 10 digits)
     * for fuzzy comparison across omitted or differently formatted country codes.
     */
    public static String extractNationalDigits(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = NON_DIGIT_PATTERN.matcher(raw).replaceAll("");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() >= 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }

    /**
     * Checks if two phone numbers refer to the same phone number, considering
     * country codes, whitespace, brackets, hyphens, and leading zero variations.
     */
    public static boolean isDuplicate(String phoneA, String phoneB) {
        if (phoneA == null || phoneB == null) {
            return false;
        }
        String normA = normalize(phoneA);
        String normB = normalize(phoneB);

        if (normA == null || normB == null) {
            return false;
        }

        // Direct canonical E.164 equality
        if (normA.equalsIgnoreCase(normB)) {
            return true;
        }

        // National significant digits check (handles +919876543210 vs 9876543210 or 09876543210)
        String natA = extractNationalDigits(phoneA);
        String natB = extractNationalDigits(phoneB);

        if (natA != null && natB != null && natA.length() == 10 && natA.equals(natB)) {
            return true;
        }

        return false;
    }

    /**
     * Validates whether a normalized or raw phone number contains a valid digit count (7 to 15 digits per ITU-T E.164).
     */
    public static boolean isValidPhoneNumber(String raw) {
        if (raw == null) {
            return false;
        }
        String digits = NON_DIGIT_PATTERN.matcher(raw).replaceAll("");
        return digits.length() >= 7 && digits.length() <= 15;
    }
}

