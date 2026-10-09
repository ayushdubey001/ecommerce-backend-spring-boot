package com.ayush.ecommerce_backend.service;


import com.ayush.ecommerce_backend.entity.CustomUserDetails;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Users user  = userRepository.findByEmail(username)
                .orElseThrow(()->new UsernameNotFoundException("User Not found with Username : " + username));

        return new CustomUserDetails(user);
    }
}
