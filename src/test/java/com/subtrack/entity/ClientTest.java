package com.subtrack.entity;

import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Client} entity.
 * Covers getFullName logic and equals/hashCode contract.
 */
class ClientTest {

    // ---------------------------------------------------------------
    // getFullName
    // ---------------------------------------------------------------
    @Test
    @DisplayName("getFullName should return 'firstName lastName' when both are set")
    void fullName_bothNames() {
        Client c = new Client();
        c.setFirstName("John");
        c.setLastName("Doe");
        assertEquals("John Doe", c.getFullName());
    }

    @Test
    @DisplayName("getFullName should return firstName only when lastName is null")
    void fullName_firstNameOnly() {
        Client c = new Client();
        c.setFirstName("John");
        c.setLastName(null);
        assertEquals("John", c.getFullName());
    }

    @Test
    @DisplayName("getFullName should return lastName only when firstName is null")
    void fullName_lastNameOnly() {
        Client c = new Client();
        c.setFirstName(null);
        c.setLastName("Doe");
        assertEquals("Doe", c.getFullName());
    }

    @Test
    @DisplayName("getFullName should return email when both names are null")
    void fullName_noNames_returnsEmail() {
        Client c = new Client();
        c.setEmail("test@example.com");
        c.setFirstName(null);
        c.setLastName(null);
        assertEquals("test@example.com", c.getFullName());
    }

    // ---------------------------------------------------------------
    // equals / hashCode
    // ---------------------------------------------------------------
    @Test
    @DisplayName("equals should return true for same UUID")
    void equals_sameId_isTrue() {
        UUID id = UUID.randomUUID();
        Client c1 = new Client();
        Client c2 = new Client();
        c1.setId(id);
        c2.setId(id);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    @DisplayName("equals should return false for different UUIDs")
    void equals_differentId_isFalse() {
        Client c1 = new Client();
        Client c2 = new Client();
        c1.setId(UUID.randomUUID());
        c2.setId(UUID.randomUUID());
        assertNotEquals(c1, c2);
    }

    @Test
    @DisplayName("equals should return false compared to null")
    void equals_null_isFalse() {
        Client c1 = new Client();
        c1.setId(UUID.randomUUID());
        assertNotEquals(null, c1);
    }

    @Test
    @DisplayName("equals should return false compared to different type")
    void equals_differentType_isFalse() {
        Client c = new Client();
        c.setId(UUID.randomUUID());
        assertNotEquals("a string", c);
    }

    // ---------------------------------------------------------------
    // Default field values
    // ---------------------------------------------------------------
    @Test
    @DisplayName("new Client should have default accountType B2C")
    void defaults_accountType() {
        Client c = new Client();
        assertEquals(AccountType.B2C, c.getAccountType());
    }

    @Test
    @DisplayName("new Client should have default role CLIENT")
    void defaults_role() {
        Client c = new Client();
        assertEquals(Role.CLIENT, c.getRole());
    }

    @Test
    @DisplayName("new Client should have isActive=true by default")
    void defaults_isActive() {
        Client c = new Client();
        assertTrue(c.getIsActive());
    }
}
