package com.ijspast.journal.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PhoneNumberUtilsTest {

    @Test
    @DisplayName("Should normalize Indian phone numbers with various formats and whitespace")
    void testNormalizeIndianNumbers() {
        assertEquals("+919876543210", PhoneNumberUtils.normalize("  +91 98765 43210  "));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("+91-98765-43210"));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("09876543210"));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("9876543210"));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("+91(98765) 43210"));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("00919876543210"));
    }

    @Test
    @DisplayName("Should normalize US/North American phone numbers with various formats")
    void testNormalizeUSNumbers() {
        assertEquals("+15551234567", PhoneNumberUtils.normalize("+1 (555) 123-4567"));
        assertEquals("+15551234567", PhoneNumberUtils.normalize("1-555-123-4567"));
        assertEquals("+15551234567", PhoneNumberUtils.normalize("+1.555.123.4567"));
        assertEquals("+15551234567", PhoneNumberUtils.normalize("15551234567"));
    }

    @Test
    @DisplayName("Should detect duplicates across formatting differences and country code prefixes")
    void testIsDuplicate() {
        assertTrue(PhoneNumberUtils.isDuplicate("+91 98765 43210", "09876543210"));
        assertTrue(PhoneNumberUtils.isDuplicate("9876543210", "+91-98765-43210"));
        assertTrue(PhoneNumberUtils.isDuplicate("+91 (987) 654-3210", "+919876543210"));

        assertTrue(PhoneNumberUtils.isDuplicate("+1 (555) 123-4567", "555-123-4567"));
        assertTrue(PhoneNumberUtils.isDuplicate("+15551234567", "1-555-123-4567"));

        assertFalse(PhoneNumberUtils.isDuplicate("+91 98765 43210", "+91 91234 56789"));
        assertFalse(PhoneNumberUtils.isDuplicate(null, "+91 98765 43210"));
        assertFalse(PhoneNumberUtils.isDuplicate("+91 98765 43210", ""));
    }

    @Test
    @DisplayName("Should handle null, blank, and invalid phone values gracefully")
    void testNullAndBlank() {
        assertNull(PhoneNumberUtils.normalize(null));
        assertNull(PhoneNumberUtils.normalize(""));
        assertNull(PhoneNumberUtils.normalize("    "));
        assertNull(PhoneNumberUtils.normalize("---"));

        assertFalse(PhoneNumberUtils.isValidPhoneNumber(null));
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("123")); // too short
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("1234567890123456789")); // too long
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("+91 98765 43210"));
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("+1 555 123 4567"));
    }
}

