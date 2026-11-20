package com.proj.ScholarAssist.security;

import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ScholarAssistUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public ScholarAssistUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User account = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(account.getEmail())
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }
}