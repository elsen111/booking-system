package com.bookingapi.auth.service;

import com.bookingapi.auth.dto.LoginRequest;
import com.bookingapi.auth.dto.RefreshTokenRequest;
import com.bookingapi.auth.dto.RegisterRequest;
import com.bookingapi.auth.dto.TokenResponse;
import com.bookingapi.auth.service.RefreshTokenService.RotatedToken;
import com.bookingapi.common.exception.ConflictException;
import com.bookingapi.common.exception.UnauthorizedException;
import com.bookingapi.config.properties.JwtProperties;
import com.bookingapi.security.jwt.JwtService;
import com.bookingapi.user.domain.Role;
import com.bookingapi.user.domain.User;
import com.bookingapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        return toResponse(user, refreshTokenService.issue(user));
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return toResponse(user, refreshTokenService.issue(user));
    }

    // Intentionally NOT @Transactional: RefreshTokenService.rotate owns the transaction
    // (an outer one would roll back the "revoke all on reuse" write when the exception propagates).
    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        RotatedToken rotated = refreshTokenService.rotate(request.refreshToken());
        return toResponse(rotated.user(), rotated.refreshToken());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private TokenResponse toResponse(User user, String refreshToken) {
        return new TokenResponse(
                jwtService.generateAccessToken(user),
                refreshToken,
                BEARER,
                jwtProperties.accessTokenTtl().toSeconds());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}