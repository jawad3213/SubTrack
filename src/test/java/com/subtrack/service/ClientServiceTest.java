package com.subtrack.service;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import com.subtrack.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClientService}.
 * All DAO interactions are mocked – no database required.
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientDAO clientDAO;

    @InjectMocks
    private ClientService clientService;

    private Client sampleClient;
    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sampleClient = new Client();
        sampleClient.setId(clientId);
        sampleClient.setEmail("test@example.com");
        sampleClient.setPassword(PasswordUtil.hashPassword("StrongPass1"));
        sampleClient.setFirstName("John");
        sampleClient.setLastName("Doe");
        sampleClient.setAccountType(AccountType.B2C);
        sampleClient.setRole(Role.CLIENT);
        sampleClient.setIsActive(true);
    }

    // ---------------------------------------------------------------
    // registerClient
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("registerClient()")
    class RegisterClient {

        @Test
        @DisplayName("should register a new client successfully")
        void register_newClient_success() {
            when(clientDAO.existsByEmail("new@example.com")).thenReturn(false);

            Client result = clientService.registerClient(
                    "new@example.com", "ValidPass1", "Jane", "Doe", AccountType.B2C);

            assertNotNull(result);
            assertEquals("new@example.com", result.getEmail());
            assertEquals("Jane", result.getFirstName());
            assertEquals(Role.CLIENT, result.getRole());
            assertTrue(result.getIsActive());
            verify(clientDAO).create(any(Client.class));
        }

        @Test
        @DisplayName("should throw when email is already registered")
        void register_duplicateEmail_throws() {
            when(clientDAO.existsByEmail("test@example.com")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clientService.registerClient(
                            "test@example.com", "ValidPass1", "A", "B", AccountType.B2C));
            assertTrue(ex.getMessage().contains("already registered"));
            verify(clientDAO, never()).create(any());
        }

        @Test
        @DisplayName("should throw when password is too weak")
        void register_weakPassword_throws() {
            when(clientDAO.existsByEmail("new@example.com")).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clientService.registerClient(
                            "new@example.com", "weak", "A", "B", AccountType.B2C));
            assertTrue(ex.getMessage().contains("Password must be"));
            verify(clientDAO, never()).create(any());
        }

        @Test
        @DisplayName("should default to B2C when accountType is null")
        void register_nullAccountType_defaultsToB2C() {
            when(clientDAO.existsByEmail("x@x.com")).thenReturn(false);

            Client result = clientService.registerClient(
                    "x@x.com", "ValidPass1", "X", "Y", null);
            assertEquals(AccountType.B2C, result.getAccountType());
        }
    }

    // ---------------------------------------------------------------
    // authenticate
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("authenticate()")
    class Authenticate {

        @Test
        @DisplayName("should return a client for valid credentials")
        void authenticate_validCredentials_returnsClient() {
            String rawPassword = "StrongPass1";
            sampleClient.setPassword(PasswordUtil.hashPassword(rawPassword));
            when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(sampleClient));

            Optional<Client> result = clientService.authenticate("test@example.com", rawPassword);

            assertTrue(result.isPresent());
            assertEquals(clientId, result.get().getId());
        }

        @Test
        @DisplayName("should return empty for wrong password")
        void authenticate_wrongPassword_returnsEmpty() {
            when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(sampleClient));

            Optional<Client> result = clientService.authenticate("test@example.com", "WrongPass9");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty for non-existent email")
        void authenticate_unknownEmail_returnsEmpty() {
            when(clientDAO.findByEmail("unknown@x.com")).thenReturn(Optional.empty());

            Optional<Client> result = clientService.authenticate("unknown@x.com", "pass");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should throw when the account is deactivated")
        void authenticate_deactivatedAccount_throws() {
            sampleClient.setIsActive(false);
            when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(sampleClient));

            assertThrows(com.subtrack.exception.UserFacingException.class,
                    () -> clientService.authenticate("test@example.com", "StrongPass1"));
        }

        @Test
        @DisplayName("wrong password on a deactivated account looks like any failed login")
        void authenticate_deactivatedAccount_wrongPassword_returnsEmpty() {
            sampleClient.setIsActive(false);
            when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(sampleClient));

            assertTrue(clientService.authenticate("test@example.com", "WrongPass1").isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // deactivateClient / activateClient
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("deactivateClient / activateClient")
    class ActivationToggle {

        @Test
        @DisplayName("deactivateClient should set isActive to false")
        void deactivate_setsActiveToFalse() {
            when(clientDAO.findById(clientId)).thenReturn(Optional.of(sampleClient));

            clientService.deactivateClient(clientId);

            assertFalse(sampleClient.getIsActive());
            verify(clientDAO).update(sampleClient);
        }

        @Test
        @DisplayName("activateClient should set isActive to true")
        void activate_setsActiveToTrue() {
            sampleClient.setIsActive(false);
            when(clientDAO.findById(clientId)).thenReturn(Optional.of(sampleClient));

            clientService.activateClient(clientId);

            assertTrue(sampleClient.getIsActive());
            verify(clientDAO).update(sampleClient);
        }

        @Test
        @DisplayName("deactivateClient should do nothing if client not found")
        void deactivate_notFound_noOp() {
            when(clientDAO.findById(clientId)).thenReturn(Optional.empty());

            clientService.deactivateClient(clientId);

            verify(clientDAO, never()).update(any());
        }
    }

    // ---------------------------------------------------------------
    // changePassword
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("should update the password when current password is correct")
        void changePassword_correctCurrent_updatesHash() {
            String rawPassword = "CurrentPass1";
            sampleClient.setPassword(PasswordUtil.hashPassword(rawPassword));
            when(clientDAO.findById(clientId)).thenReturn(Optional.of(sampleClient));

            clientService.changePassword(clientId, rawPassword, "NewSecure1");

            verify(clientDAO).update(sampleClient);
            assertTrue(PasswordUtil.verifyPassword("NewSecure1", sampleClient.getPassword()));
        }

        @Test
        @DisplayName("should throw when current password is wrong")
        void changePassword_wrongCurrent_throws() {
            sampleClient.setPassword(PasswordUtil.hashPassword("RealPass1"));
            when(clientDAO.findById(clientId)).thenReturn(Optional.of(sampleClient));

            assertThrows(IllegalArgumentException.class,
                    () -> clientService.changePassword(clientId, "WrongCurrent", "NewPass1"));
        }

        @Test
        @DisplayName("should throw when client is not found")
        void changePassword_notFound_throws() {
            when(clientDAO.findById(clientId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> clientService.changePassword(clientId, "a", "b"));
        }
    }

    // ---------------------------------------------------------------
    // Delegation tests (simple pass-through)
    // ---------------------------------------------------------------
    @Test
    @DisplayName("findAll should delegate to DAO")
    void findAll_delegatesToDAO() {
        when(clientDAO.findAll()).thenReturn(List.of(sampleClient));
        List<Client> result = clientService.findAll();
        assertEquals(1, result.size());
        verify(clientDAO).findAll();
    }


    @Test
    @DisplayName("deleteClient should delete if found")
    void deleteClient_found_deletes() {
        when(clientDAO.findById(clientId)).thenReturn(Optional.of(sampleClient));
        clientService.deleteClient(clientId);
        verify(clientDAO).delete(sampleClient);
    }

    // ---------------------------------------------------------------
    // password reset
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("password reset")
    class PasswordReset {

        @Test
        @DisplayName("issuing a token keeps the current password and stores only a hash")
        void createTokenKeepsPassword() {
            String originalHash = sampleClient.getPassword();
            when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(sampleClient));

            String token = clientService.createPasswordResetToken("test@example.com").orElseThrow();

            assertEquals(originalHash, sampleClient.getPassword());
            assertEquals(PasswordUtil.sha256Hex(token), sampleClient.getPasswordResetTokenHash());
            assertNotEquals(token, sampleClient.getPasswordResetTokenHash());
            assertTrue(sampleClient.getPasswordResetExpiresAt().isAfter(java.time.LocalDateTime.now()));
        }

        @Test
        @DisplayName("no token for unknown or inactive accounts")
        void noTokenForUnknownOrInactive() {
            when(clientDAO.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
            assertTrue(clientService.createPasswordResetToken("nobody@example.com").isEmpty());

            sampleClient.setIsActive(false);
            when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(sampleClient));
            assertTrue(clientService.createPasswordResetToken("test@example.com").isEmpty());
            verify(clientDAO, never()).update(any());
        }

        @Test
        @DisplayName("valid token resets the password and is consumed")
        void resetWithValidToken() {
            sampleClient.setPasswordResetTokenHash(PasswordUtil.sha256Hex("tok"));
            sampleClient.setPasswordResetExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
            when(clientDAO.findByPasswordResetTokenHash(PasswordUtil.sha256Hex("tok")))
                .thenReturn(Optional.of(sampleClient));

            clientService.resetPasswordWithToken("tok", "NewStrong1");

            assertTrue(PasswordUtil.verifyPassword("NewStrong1", sampleClient.getPassword()));
            assertNull(sampleClient.getPasswordResetTokenHash());
            assertNull(sampleClient.getPasswordResetExpiresAt());
        }

        @Test
        @DisplayName("expired token is rejected")
        void expiredTokenRejected() {
            sampleClient.setPasswordResetTokenHash(PasswordUtil.sha256Hex("tok"));
            sampleClient.setPasswordResetExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
            when(clientDAO.findByPasswordResetTokenHash(PasswordUtil.sha256Hex("tok")))
                .thenReturn(Optional.of(sampleClient));

            assertFalse(clientService.isPasswordResetTokenValid("tok"));
            assertThrows(IllegalArgumentException.class,
                () -> clientService.resetPasswordWithToken("tok", "NewStrong1"));
        }

        @Test
        @DisplayName("blank token is rejected without a lookup")
        void blankTokenRejected() {
            assertFalse(clientService.isPasswordResetTokenValid(""));
            assertFalse(clientService.isPasswordResetTokenValid(null));
            verifyNoInteractions(clientDAO);
        }
    }
}
