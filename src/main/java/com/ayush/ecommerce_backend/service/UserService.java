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
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CartService cartService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;


    // =========================
    // REGISTER
    // =========================

    @Transactional
    public UserResponse createUser(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

        /*
         * Check the normalized email.
         */
        if (userRepository.existsByEmail(email)) {

            throw new DuplicateResourceException(
                    "User already exists with mail id : " + email
            );
        }

        Role role =
                roleRepository.findByName("ROLE_USER");

        if (role == null) {

            throw new ResourceNotFoundException(
                    "ROLE_USER not found"
            );
        }

        Users user = new Users();

        user.setName(request.getName());
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setEnabled(true);

        user.getRoleList().add(role);

        Users savedUser =
                userRepository.save(user);

        /*
         * Every registered user gets a cart.
         */
        cartService.createCart(savedUser);

        return entityToResponse(savedUser);
    }


    // =========================
    // GET CURRENT USER
    // =========================

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(
            String email
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        Users user =
                userRepository.findByEmail(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with email "
                                                + normalizedEmail
                                )
                        );

        return entityToResponse(user);
    }


    // =========================
    // UPDATE CURRENT USER
    // =========================

    @Transactional
    public UserResponse updateCurrentUser(
            String currentEmail,
            UpdateUserRequest request
    ) {

        String normalizedCurrentEmail =
                normalizeEmail(currentEmail);

        Users user =
                userRepository.findByEmail(
                                normalizedCurrentEmail
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with email "
                                                + normalizedCurrentEmail
                                )
                        );


        String newEmail =
                normalizeEmail(request.getEmail());


        /*
         * Check whether another user already
         * owns this email.
         *
         * The current user's own email is ignored.
         */
        if (userRepository.existsByEmailAndIdNot(
                newEmail,
                user.getId()
        )) {

            throw new DuplicateResourceException(
                    "User already exists with mail id : "
                            + newEmail
            );
        }


        user.setName(request.getName());
        user.setEmail(newEmail);

        return entityToResponse(user);
    }


    // =========================
    // CHANGE PASSWORD
    // =========================

    @Transactional
    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        Users user =
                userRepository.findByEmail(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with email "
                                                + normalizedEmail
                                )
                        );


        /*
         * Verify current password.
         */
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }


        /*
         * New password must be different.
         */
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }


        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );
    }


    // =========================
    // NORMALIZE EMAIL
    // =========================

    private String normalizeEmail(String email) {

        if (email == null) {
            throw new IllegalArgumentException(
                    "Email cannot be null"
            );
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }


    // =========================
    // ENTITY -> RESPONSE
    // =========================

    private UserResponse entityToResponse(
            Users user
    ) {

        UserResponse response =
                new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setEnabled(user.isEnabled());

        response.setRoles(
                user.getRoleList()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );

        return response;
    }
}