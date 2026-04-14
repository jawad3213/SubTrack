package com.subtrack.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashPassword_ReturnsBCryptHash() {
        String hashed = PasswordUtil.hashPassword("Password123");
        
        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2a$"));
        assertTrue(hashed.length() > 20);
    }

    @Test
    void hashPassword_DifferentHashesForSamePassword() {
        String hash1 = PasswordUtil.hashPassword("Password123");
        String hash2 = PasswordUtil.hashPassword("Password123");
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verifyPassword_CorrectPassword_ReturnsTrue() {
        String hashed = PasswordUtil.hashPassword("Password123");
        
        assertTrue(PasswordUtil.verifyPassword("Password123", hashed));
    }

    @Test
    void verifyPassword_WrongPassword_ReturnsFalse() {
        String hashed = PasswordUtil.hashPassword("Password123");
        
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", hashed));
    }

    @Test
    void verifyPassword_NullPlainPassword_ReturnsFalse() {
        String hashed = PasswordUtil.hashPassword("Password123");
        
        assertFalse(PasswordUtil.verifyPassword(null, hashed));
    }

    @Test
    void verifyPassword_NullHashedPassword_ReturnsFalse() {
        assertFalse(PasswordUtil.verifyPassword("Password123", null));
    }

    @Test
    void verifyPassword_BothNull_ReturnsFalse() {
        assertFalse(PasswordUtil.verifyPassword(null, null));
    }

    @Test
    void isPasswordStrong_ValidPassword_ReturnsTrue() {
        assertTrue(PasswordUtil.isPasswordStrong("Password123"));
    }

    @Test
    void isPasswordStrong_MinimumLength_ReturnsTrue() {
        assertTrue(PasswordUtil.isPasswordStrong("Ab123456"));
    }

    @Test
    void isPasswordStrong_TooShort_ReturnsFalse() {
        assertFalse(PasswordUtil.isPasswordStrong("Pass123"));
    }

    @Test
    void isPasswordStrong_MissingUppercase_ReturnsFalse() {
        assertFalse(PasswordUtil.isPasswordStrong("password123"));
    }

    @Test
    void isPasswordStrong_MissingLowercase_ReturnsFalse() {
        assertFalse(PasswordUtil.isPasswordStrong("PASSWORD123"));
    }

    @Test
    void isPasswordStrong_MissingDigit_ReturnsFalse() {
        assertFalse(PasswordUtil.isPasswordStrong("PasswordABC"));
    }

    @Test
    void isPasswordStrong_NullPassword_ReturnsFalse() {
        assertFalse(PasswordUtil.isPasswordStrong(null));
    }

    @Test
    void isPasswordStrong_EmptyPassword_ReturnsFalse() {
        assertFalse(PasswordUtil.isPasswordStrong(""));
    }

    @Test
    void isPasswordStrong_Exactly8CharsWithAllRequirements_ReturnsTrue() {
        assertTrue(PasswordUtil.isPasswordStrong("Abcdefg1"));
    }

    @Test
    void isPasswordStrong_LongerPasswordWithRequirements_ReturnsTrue() {
        assertTrue(PasswordUtil.isPasswordStrong("MyVeryStrongPassword123"));
    }

    @Test
    void isPasswordStrong_WithSpecialChar_ReturnsTrue() {
        assertTrue(PasswordUtil.isPasswordStrong("Password123!"));
    }

    @Test
    void hashPassword_NullPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            PasswordUtil.hashPassword(null));
    }

    @Test
    void hashPassword_EmptyPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            PasswordUtil.hashPassword(""));
    }

    @Test
    void generateRandomToken_ReturnsToken() {
        String token = PasswordUtil.generateRandomToken();
        
        assertNotNull(token);
        assertTrue(token.length() > 20);
    }

    @Test
    void generateRandomToken_DifferentTokens() {
        String token1 = PasswordUtil.generateRandomToken();
        String token2 = PasswordUtil.generateRandomToken();
        
        assertNotEquals(token1, token2);
    }

    @Test
    void generateRandomToken_UrlSafe() {
        String token = PasswordUtil.generateRandomToken();
        
        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));
    }

    @Test
    void verifyPassword_InvalidHashFormat_ReturnsFalse() {
        assertFalse(PasswordUtil.verifyPassword("password", "invalid_hash"));
    }

    @Test
    void verifyPassword_TruncatedHash_ReturnsFalse() {
        assertFalse(PasswordUtil.verifyPassword("password", "$2a$10$abc"));
    }
}