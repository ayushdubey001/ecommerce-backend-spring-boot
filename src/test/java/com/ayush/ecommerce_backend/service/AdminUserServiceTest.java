package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.AdminUserResponse;
import com.ayush.ecommerce_backend.dto.UpdateUserRoleRequest;
import com.ayush.ecommerce_backend.entity.Role;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.exception.ConflictException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.RoleRepository;
import com.ayush.ecommerce_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @InjectMocks private AdminUserService adminUserService;

    private Users user;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role(1, "ROLE_USER");
        adminRole = new Role(2, "ROLE_ADMIN");
        user = new Users();
        user.setId(10);
        user.setName("Ayush");
        user.setEmail("ayush@example.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.setRoleList(new HashSet<>(Set.of(userRole)));
    }

    @Test
    void getAllUsers_withoutSearch_returnsMappedPage() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        Page<AdminUserResponse> result = adminUserService.getAllUsers(0, 10, "name", "asc", null);
        assertEquals(1, result.getTotalElements());
        assertEquals("Ayush", result.getContent().get(0).getName());
        assertEquals(Set.of("ROLE_USER"), result.getContent().get(0).getRoles());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllUsers_searchesByTrimmedName() {
        when(userRepository.findByNameContainingIgnoreCase(eq("Ayush"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        assertEquals(1, adminUserService.getAllUsers(0, 10, "email", "desc", " Ayush ").getTotalElements());
        verify(userRepository).findByNameContainingIgnoreCase(eq("Ayush"), any(Pageable.class));
    }

    @Test void getAllUsers_rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> adminUserService.getAllUsers(-1, 10, "name", "asc", null));
    }
    @Test void getAllUsers_rejectsZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> adminUserService.getAllUsers(0, 0, "name", "asc", null));
    }
    @Test void getAllUsers_rejectsSizeAbove100() {
        assertThrows(IllegalArgumentException.class, () -> adminUserService.getAllUsers(0, 101, "name", "asc", null));
    }
    @Test void getAllUsers_rejectsInvalidSortField() {
        assertThrows(IllegalArgumentException.class, () -> adminUserService.getAllUsers(0, 10, "password", "asc", null));
    }
    @Test void getAllUsers_rejectsInvalidDirection() {
        assertThrows(IllegalArgumentException.class, () -> adminUserService.getAllUsers(0, 10, "name", "sideways", null));
    }

    @Test
    void getUser_returnsMappedUser() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        AdminUserResponse result = adminUserService.getUser(10);
        assertEquals(10, result.getId());
        assertEquals("Ayush", result.getName());
        assertEquals("ayush@example.com", result.getEmail());
        assertTrue(result.isEnabled());
    }

    @Test
    void getUser_throwsWhenMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminUserService.getUser(99));
    }

    @Test
    void updateUserStatus_disablesRegularUser() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        AdminUserResponse result = adminUserService.updateUserStatus(10, false);
        assertFalse(result.isEnabled());
        assertFalse(user.isEnabled());
        verify(userRepository, never()).countEnabledUsersByRole(anyString());
    }

    @Test
    void updateUserStatus_enablesDisabledUser() {
        user.setEnabled(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        assertTrue(adminUserService.updateUserStatus(10, true).isEnabled());
    }

    @Test
    void updateUserStatus_sameStateDoesNotChangeAnything() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        assertTrue(adminUserService.updateUserStatus(10, true).isEnabled());
        verify(userRepository, never()).countEnabledUsersByRole(anyString());
    }

    @Test
    void updateUserStatus_cannotDisableLastEnabledAdmin() {
        user.setRoleList(new HashSet<>(Set.of(adminRole)));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(userRepository.countEnabledUsersByRole("ROLE_ADMIN")).thenReturn(1L);
        assertThrows(ConflictException.class, () -> adminUserService.updateUserStatus(10, false));
        assertTrue(user.isEnabled());
    }

    @Test
    void updateUserStatus_canDisableAdminWhenAnotherEnabledAdminExists() {
        user.setRoleList(new HashSet<>(Set.of(adminRole)));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(userRepository.countEnabledUsersByRole("ROLE_ADMIN")).thenReturn(2L);
        assertFalse(adminUserService.updateUserStatus(10, false).isEnabled());
    }

    @Test
    void updateUserRole_changesUserToAdmin() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole);
        AdminUserResponse result = adminUserService.updateUserRole(10, new UpdateUserRoleRequest("ROLE_ADMIN"));
        assertEquals(Set.of("ROLE_ADMIN"), result.getRoles());
        assertEquals(Set.of(adminRole), user.getRoleList());
    }

    @Test
    void updateUserRole_normalizesRoleToUppercase() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole);
        adminUserService.updateUserRole(10, new UpdateUserRoleRequest("role_admin"));
        verify(roleRepository).findByName("ROLE_ADMIN");
    }

    @Test
    void updateUserRole_rejectsUnsupportedRole() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> adminUserService.updateUserRole(10, new UpdateUserRoleRequest("ROLE_MANAGER")));
    }

    @Test
    void updateUserRole_throwsWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminUserService.updateUserRole(99, new UpdateUserRoleRequest("ROLE_ADMIN")));
    }

    @Test
    void updateUserRole_throwsWhenRoleDoesNotExistInDatabase() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> adminUserService.updateUserRole(10, new UpdateUserRoleRequest("ROLE_ADMIN")));
    }

    @Test
    void updateUserRole_doesNotLookupRoleIfAlreadyHasOnlyRequestedRole() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        AdminUserResponse result = adminUserService.updateUserRole(10, new UpdateUserRoleRequest("ROLE_USER"));
        assertEquals(Set.of("ROLE_USER"), result.getRoles());
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void updateUserRole_cannotRemoveRoleFromLastEnabledAdmin() {
        user.setRoleList(new HashSet<>(Set.of(adminRole)));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(userRepository.countEnabledUsersByRole("ROLE_ADMIN")).thenReturn(1L);
        assertThrows(ConflictException.class, () -> adminUserService.updateUserRole(10, new UpdateUserRoleRequest("ROLE_USER")));
        assertEquals(Set.of(adminRole), user.getRoleList());
    }

    @Test
    void deleteUser_softDeletesRegularUser() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        adminUserService.deleteUser(10);
        assertFalse(user.isEnabled());
        verify(userRepository, never()).delete(any(Users.class));
    }

    @Test
    void deleteUser_isNoOpIfAlreadyDisabled() {
        user.setEnabled(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        adminUserService.deleteUser(10);
        assertFalse(user.isEnabled());
        verify(userRepository, never()).countEnabledUsersByRole(anyString());
    }

    @Test
    void deleteUser_cannotDeleteLastEnabledAdmin() {
        user.setRoleList(new HashSet<>(Set.of(adminRole)));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(userRepository.countEnabledUsersByRole("ROLE_ADMIN")).thenReturn(1L);
        assertThrows(ConflictException.class, () -> adminUserService.deleteUser(10));
        assertTrue(user.isEnabled());
    }

    @Test
    void deleteUser_canSoftDeleteAdminWhenAnotherAdminExists() {
        user.setRoleList(new HashSet<>(Set.of(adminRole)));
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(userRepository.countEnabledUsersByRole("ROLE_ADMIN")).thenReturn(2L);
        adminUserService.deleteUser(10);
        assertFalse(user.isEnabled());
    }

    @Test
    void deleteUser_throwsWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminUserService.deleteUser(99));
    }
}
