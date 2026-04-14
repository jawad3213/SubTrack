package com.subtrack.service;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import com.subtrack.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientDAO clientDAO;

    @InjectMocks
    private ClientService clientService;

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = new Client();
        testClient.setId(UUID.randomUUID());
        testClient.setEmail("test@example.com");
        testClient.setPassword(PasswordUtil.hashPassword("Password123"));
        testClient.setFirstName("John");
        testClient.setLastName("Doe");
        testClient.setAccountType(AccountType.B2C);
        testClient.setRole(Role.CLIENT);
        testClient.setIsActive(true);
    }

    @Test
    void registerClient_Success() {
        when(clientDAO.existsByEmail("new@example.com")).thenReturn(false);
        when(clientDAO.existsByEmail(anyString())).thenReturn(false);
        doNothing().when(clientDAO).create(any(Client.class));

        Client result = clientService.registerClient("new@example.com", "Password123", "John", "Doe", AccountType.B2C);

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(AccountType.B2C, result.getAccountType());
        assertEquals(Role.CLIENT, result.getRole());
        verify(clientDAO).create(any(Client.class));
    }

    @Test
    void registerClient_EmailAlreadyExists_ThrowsException() {
        when(clientDAO.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.registerClient("existing@example.com", "Password123", "John", "Doe", AccountType.B2C));
    }

    @Test
    void registerClient_WeakPassword_ThrowsException() {
        when(clientDAO.existsByEmail(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.registerClient("new@example.com", "weak", "John", "Doe", AccountType.B2C));
    }

    @Test
    void registerClient_PasswordTooShort_ThrowsException() {
        when(clientDAO.existsByEmail(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.registerClient("new@example.com", "Pass123", "John", "Doe", AccountType.B2C));
    }

    @Test
    void registerClient_MissingUppercase_ThrowsException() {
        when(clientDAO.existsByEmail(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.registerClient("new@example.com", "password123", "John", "Doe", AccountType.B2C));
    }

    @Test
    void registerClient_MissingLowercase_ThrowsException() {
        when(clientDAO.existsByEmail(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.registerClient("new@example.com", "PASSWORD123", "John", "Doe", AccountType.B2C));
    }

    @Test
    void registerClient_MissingDigit_ThrowsException() {
        when(clientDAO.existsByEmail(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.registerClient("new@example.com", "PasswordABC", "John", "Doe", AccountType.B2C));
    }

    @Test
    void authenticate_Success() {
        when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(testClient));

        Optional<Client> result = clientService.authenticate("test@example.com", "Password123");

        assertTrue(result.isPresent());
        assertEquals(testClient.getEmail(), result.get().getEmail());
    }

    @Test
    void authenticate_WrongPassword_ReturnsEmpty() {
        when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(testClient));

        Optional<Client> result = clientService.authenticate("test@example.com", "WrongPassword");

        assertTrue(result.isEmpty());
    }

    @Test
    void authenticate_UserNotFound_ReturnsEmpty() {
        when(clientDAO.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        Optional<Client> result = clientService.authenticate("notfound@example.com", "Password123");

        assertTrue(result.isEmpty());
    }

    @Test
    void authenticate_DeactivatedUser_ThrowsException() {
        testClient.setIsActive(false);
        when(clientDAO.findByEmail("test@example.com")).thenReturn(Optional.of(testClient));

        assertThrows(IllegalStateException.class, () -> 
            clientService.authenticate("test@example.com", "Password123"));
    }

    @Test
    void isEmailTaken_ReturnsTrue() {
        when(clientDAO.existsByEmail("taken@example.com")).thenReturn(true);

        assertTrue(clientService.isEmailTaken("taken@example.com"));
    }

    @Test
    void isEmailTaken_ReturnsFalse() {
        when(clientDAO.existsByEmail("available@example.com")).thenReturn(false);

        assertFalse(clientService.isEmailTaken("available@example.com"));
    }

    @Test
    void deactivateClient_Success() {
        when(clientDAO.findById(testClient.getId())).thenReturn(Optional.of(testClient));
        doNothing().when(clientDAO).update(any(Client.class));

        clientService.deactivateClient(testClient.getId());

        assertFalse(testClient.getIsActive());
        verify(clientDAO).update(any(Client.class));
    }

    @Test
    void activateClient_Success() {
        testClient.setIsActive(false);
        when(clientDAO.findById(testClient.getId())).thenReturn(Optional.of(testClient));
        doNothing().when(clientDAO).update(any(Client.class));

        clientService.activateClient(testClient.getId());

        assertTrue(testClient.getIsActive());
        verify(clientDAO).update(any(Client.class));
    }

    @Test
    void changePassword_Success() {
        when(clientDAO.findById(testClient.getId())).thenReturn(Optional.of(testClient));
        doNothing().when(clientDAO).update(any(Client.class));

        clientService.changePassword(testClient.getId(), "Password123", "NewPassword456");

        assertTrue(PasswordUtil.verifyPassword("NewPassword456", testClient.getPassword()));
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        when(clientDAO.findById(testClient.getId())).thenReturn(Optional.of(testClient));

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.changePassword(testClient.getId(), "WrongPassword", "NewPassword456"));
    }

    @Test
    void changePassword_ClientNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(clientDAO.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            clientService.changePassword(randomId, "Password123", "NewPassword456"));
    }
}