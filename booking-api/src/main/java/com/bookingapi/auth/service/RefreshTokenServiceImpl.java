package com.bookingapi.auth.service;

import com.bookingapi.auth.domain.RefreshToken;
import com.bookingapi.auth.repository.RefreshTokenRepository;
import com.bookingapi.common.exception.UnauthorizedException;
import com.bookingapi.config.properties.JwtProperties;
import com.bookingapi.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plus(jwtProperties.refreshTokenTtl()))
                .revoked(false)
                .build();

        repository.save(token);

        return rawToken;
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RotatedToken rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        User user = current.getUser();

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", user.getId());
            repository.revokeAllByUserId(user.getId());
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now()) || !user.isEnabled()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        current.setRevoked(true);
        return new RotatedToken(user, issue(user));
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> token.setRevoked(true));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}