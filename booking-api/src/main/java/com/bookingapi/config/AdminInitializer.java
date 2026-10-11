package com.bookingapi.config;

import com.bookingapi.config.properties.AdminProperties;
import com.bookingapi.user.domain.Role;
import com.bookingapi.user.domain.User;
import com.bookingapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        String email = adminProperties.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode(adminProperties.password()))
                .fullName("SystemAdmin")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.info("Bootstrap admin created: {}", email);
    }
}