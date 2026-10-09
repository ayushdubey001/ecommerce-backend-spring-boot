
package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.ChangePasswordRequest;
import com.ayush.ecommerce_backend.dto.RegisterRequest;
import com.ayush.ecommerce_backend.dto.UpdateUserRequest;
import com.ayush.ecommerce_backend.dto.UserResponse;
import com.ayush.ecommerce_backend.entity.Role;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.exception.DuplicateResourceException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.RoleRepository;
import com.ayush.ecommerce_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private Users user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1);
        userRole.setName("ROLE_USER");

        user = new Users();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.getRoleList().add(userRole);
    }

    // REGISTRATION

    @Test
    void createUser_shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                " USER@EXAMPLE.COM ",
                "password123"
        );

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(userRole);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(Users.class)))
                .thenAnswer(invocation -> {
                    Users saved = invocation.getArgument(0);
                    saved.setId(1);
                    return saved;
                });

        UserResponse response = userService.createUser(request);

        assertEquals(1, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("user@example.com", response.getEmail());
        assertTrue(response.isEnabled());
        assertTrue(response.getRoles().contains("ROLE_USER"));

        verify(userRepository).save(any(Users.class));
        verify(cartService).createCart(any(Users.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void createUser_shouldNormalizeEmail() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "  USER@EXAMPLE.COM  ",
                "password123"
        );

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(userRole);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(Users.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertEquals("user@example.com", response.getEmail());
    }

    @Test
    void createUser_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "user@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(Users.class));
        verify(cartService, never()).createCart(any(Users.class));
    }

    @Test
    void createUser_shouldFailWhenDefaultRoleDoesNotExist() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "user@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(Users.class));
    }

    // GET PROFILE

    @Test
    void getCurrentUser_shouldReturnUserProfile() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getCurrentUser(" USER@EXAMPLE.COM ");

        assertEquals(1, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("user@example.com", response.getEmail());
        assertTrue(response.isEnabled());
        assertTrue(response.getRoles().contains("ROLE_USER"));
    }

    @Test
    void getCurrentUser_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getCurrentUser("missing@example.com")
        );
    }

    // UPDATE PROFILE

    @Test
    void updateCurrentUser_shouldUpdateNameAndEmail() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name",
                " UPDATED@EXAMPLE.COM "
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(
                "updated@example.com", 1))
                .thenReturn(false);

        UserResponse response = userService.updateCurrentUser(
                "user@example.com", request
        );

        assertEquals("Updated Name", response.getName());
        assertEquals("updated@example.com", response.getEmail());
    }

    @Test
    void updateCurrentUser_shouldRejectEmailUsedByAnotherUser() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name",
                "taken@example.com"
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(
                "taken@example.com", 1))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.updateCurrentUser(
                        "user@example.com", request
                )
        );

        assertEquals("user@example.com", user.getEmail());
    }

    @Test
    void updateCurrentUser_shouldThrowWhenCurrentUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name",
                "updated@example.com"
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateCurrentUser(
                        "missing@example.com", request
                )
        );
    }

    // CHANGE PASSWORD

    @Test
    void changePassword_shouldUpdatePasswordSuccessfully() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "old-password",
                "new-password"
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "old-password", "encoded-password"))
                .thenReturn(true);
        when(passwordEncoder.matches(
                "new-password", "encoded-password"))
                .thenReturn(false);
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-encoded-password");

        userService.changePassword("user@example.com", request);

        assertEquals("new-encoded-password", user.getPassword());
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void changePassword_shouldRejectIncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrong-password",
                "new-password"
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(
                        "user@example.com", request
                )
        );

        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void changePassword_shouldRejectSamePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "old-password",
                "old-password"
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "old-password", "encoded-password"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(
                        "user@example.com", request
                )
        );

        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void changePassword_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest(
                "old-password",
                "new-password"
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.changePassword(
                        "missing@example.com", request
                )
        );
    }
}
