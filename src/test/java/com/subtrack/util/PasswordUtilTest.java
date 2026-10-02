package com.subtrack.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PasswordUtil}.
 * Covers hashing, verification, strength validation, and token generation.
 */
class PasswordUtilTest {

    // ---------------------------------------------------------------
    // hashPassword
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("hashPassword()")
    class HashPassword {

        @Test
        @DisplayName("should return a BCrypt hash that starts with $2a$")
        void hashPassword_validInput_returnsBCryptHash() {
            String hash = PasswordUtil.hashPassword("SecurePass1");
            assertNotNull(hash);
            assertTrue(hash.startsWith("$2a$"), "BCrypt hash should start with $2a$");
        }

        @Test
        @DisplayName("should produce different hashes for the same password (salted)")
        void hashPassword_samePlaintext_differentHashes() {
            String hash1 = PasswordUtil.hashPassword("SecurePass1");
            String hash2 = PasswordUtil.hashPassword("SecurePass1");
            assertNotEquals(hash1, hash2, "Each hash should use a unique salt");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null password")
        void hashPassword_null_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PasswordUtil.hashPassword(null));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for empty password")
        void hashPassword_empty_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PasswordUtil.hashPassword(""));
        }
    }

    // ---------------------------------------------------------------
    // verifyPassword
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("verifyPassword()")
    class VerifyPassword {

        @Test
        @DisplayName("should return true for a matching plaintext/hash pair")
        void verifyPassword_correctPassword_returnsTrue() {
            String hash = PasswordUtil.hashPassword("MyPass123");
            assertTrue(PasswordUtil.verifyPassword("MyPass123", hash));
        }

        @Test
        @DisplayName("should return false for an incorrect plaintext")
        void verifyPassword_wrongPassword_returnsFalse() {
            String hash = PasswordUtil.hashPassword("MyPass123");
            assertFalse(PasswordUtil.verifyPassword("WrongPass", hash));
        }

        @Test
        @DisplayName("should return false when plaintext is null")
        void verifyPassword_nullPlaintext_returnsFalse() {
            assertFalse(PasswordUtil.verifyPassword(null, "$2a$12$someHash"));
        }

        @Test
        @DisplayName("should return false when hash is null")
        void verifyPassword_nullHash_returnsFalse() {
            assertFalse(PasswordUtil.verifyPassword("pass", null));
        }

        @Test
        @DisplayName("should return false for a malformed hash string")
        void verifyPassword_malformedHash_returnsFalse() {
            assertFalse(PasswordUtil.verifyPassword("pass", "not-a-bcrypt-hash"));
        }
    }

    // ---------------------------------------------------------------
    // isPasswordStrong
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("isPasswordStrong()")
    class IsPasswordStrong {

        @Test
        @DisplayName("should accept a password with upper, lower, and digit")
        void strong_validPassword_returnsTrue() {
            assertTrue(PasswordUtil.isPasswordStrong("Abcdefg1"));
        }

        @Test
        @DisplayName("should reject a null password")
        void strong_null_returnsFalse() {
            assertFalse(PasswordUtil.isPasswordStrong(null));
        }

        @Test
        @DisplayName("should reject a password shorter than 8 characters")
        void strong_tooShort_returnsFalse() {
            assertFalse(PasswordUtil.isPasswordStrong("Ab1"));
        }

        @Test
        @DisplayName("should reject a password without uppercase letters")
        void strong_noUppercase_returnsFalse() {
            assertFalse(PasswordUtil.isPasswordStrong("abcdefg1"));
        }

        @Test
        @DisplayName("should reject a password without lowercase letters")
        void strong_noLowercase_returnsFalse() {
            assertFalse(PasswordUtil.isPasswordStrong("ABCDEFG1"));
        }

        @Test
        @DisplayName("should reject a password without digits")
        void strong_noDigit_returnsFalse() {
            assertFalse(PasswordUtil.isPasswordStrong("Abcdefgh"));
        }
    }

    // ---------------------------------------------------------------
    // generateRandomToken
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("generateRandomToken()")
    class GenerateRandomToken {

        @Test
        @DisplayName("should return a non-null, non-empty token")
        void generateToken_returnsNonEmpty() {
            String token = PasswordUtil.generateRandomToken();
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("should produce unique tokens on successive calls")
        void generateToken_uniqueEachTime() {
            String t1 = PasswordUtil.generateRandomToken();
            String t2 = PasswordUtil.generateRandomToken();
            assertNotEquals(t1, t2);
        }
    }
}