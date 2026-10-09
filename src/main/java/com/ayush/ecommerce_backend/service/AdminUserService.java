package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.AdminUserResponse;
import com.ayush.ecommerce_backend.dto.UpdateUserRoleRequest;
import com.ayush.ecommerce_backend.entity.Role;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.exception.ConflictException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.RoleRepository;
import com.ayush.ecommerce_backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AdminUserService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;


    // =========================
    // GET ALL USERS
    // =========================

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction,
            String search
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        List<String> allowedSortFields = List.of(
                "id",
                "name",
                "email"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field. Allowed fields: "
                            + allowedSortFields
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection =
                    Sort.Direction.fromString(direction);

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid sort direction. Use 'asc' or 'desc'"
            );
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(sortDirection, sortBy)
                );

        Page<Users> users;

        if (search != null
                && !search.trim().isEmpty()) {

            users =
                    userRepository
                            .findByNameContainingIgnoreCase(
                                    search.trim(),
                                    pageable
                            );

        } else {

            users =
                    userRepository.findAll(pageable);
        }

        return users.map(this::entityToResponse);
    }


    // =========================
    // GET USER
    // =========================

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Integer id) {

        Users user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id "
                                                + id
                                )
                        );

        return entityToResponse(user);
    }


    // =========================
    // ENABLE / DISABLE USER
    // =========================

    @Transactional
    public AdminUserResponse updateUserStatus(
            Integer id,
            boolean enabled
    ) {

        Users user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id "
                                                + id
                                )
                        );


        /*
         * If the user is already in the requested state,
         * nothing needs to be changed.
         */
        if (user.isEnabled() == enabled) {
            return entityToResponse(user);
        }


        /*
         * Prevent disabling the last enabled ADMIN.
         */
        if (!enabled
                && hasRole(user, ROLE_ADMIN)
                && isLastEnabledAdmin(user)) {

            throw new ConflictException(
                    "Cannot disable the last enabled admin user"
            );
        }

        user.setEnabled(enabled);

        return entityToResponse(user);
    }


    // =========================
    // CHANGE USER ROLE
    // =========================

    @Transactional
    public AdminUserResponse updateUserRole(
            Integer id,
            UpdateUserRoleRequest request
    ) {

        Users user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id "
                                                + id
                                )
                        );

        String roleName =
                request.getRole()
                        .trim()
                        .toUpperCase();

        if (!roleName.equals(ROLE_USER)
                && !roleName.equals(ROLE_ADMIN)) {

            throw new IllegalArgumentException(
                    "Invalid role. Allowed roles: "
                            + ROLE_USER
                            + ", "
                            + ROLE_ADMIN
            );
        }


        /*
         * If the user already has the requested role,
         * nothing needs to be changed.
         */
        if (hasRole(user, roleName)
                && user.getRoleList().size() == 1) {

            return entityToResponse(user);
        }


        /*
         * Prevent removing the ADMIN role from
         * the last enabled ADMIN.
         */
        if (roleName.equals(ROLE_USER)
                && user.isEnabled()
                && hasRole(user, ROLE_ADMIN)
                && isLastEnabledAdmin(user)) {

            throw new ConflictException(
                    "Cannot remove the ADMIN role from the last enabled admin user"
            );
        }


        Role role =
                roleRepository.findByName(roleName);

        if (role == null) {

            throw new ResourceNotFoundException(
                    "Role not found: " + roleName
            );
        }

        user.getRoleList().clear();
        user.getRoleList().add(role);

        return entityToResponse(user);
    }


    // =========================
    // DELETE USER
    // =========================

    @Transactional
    public void deleteUser(Integer id) {

        Users user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id "
                                                + id
                                )
                        );


        /*
         * DELETE is implemented as a soft delete.
         */
        if (!user.isEnabled()) {
            return;
        }


        /*
         * Prevent deleting/disabling the last
         * enabled ADMIN.
         */
        if (hasRole(user, ROLE_ADMIN)
                && isLastEnabledAdmin(user)) {

            throw new ConflictException(
                    "Cannot delete the last enabled admin user"
            );
        }

        user.setEnabled(false);
    }


    // =========================
    // CHECK ROLE
    // =========================

    private boolean hasRole(
            Users user,
            String roleName
    ) {

        return user.getRoleList()
                .stream()
                .anyMatch(role ->
                        roleName.equals(role.getName())
                );
    }


    // =========================
    // CHECK LAST ADMIN
    // =========================

    private boolean isLastEnabledAdmin(
            Users user
    ) {

        long enabledAdminCount =
                userRepository.countEnabledUsersByRole(
                        ROLE_ADMIN
                );

        return enabledAdminCount <= 1;
    }


    // =========================
    // ENTITY -> RESPONSE
    // =========================

    private AdminUserResponse entityToResponse(
            Users user
    ) {

        AdminUserResponse response =
                new AdminUserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setEnabled(user.isEnabled());

        Set<String> roles =
                user.getRoleList()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet());

        response.setRoles(roles);

        return response;
    }
}