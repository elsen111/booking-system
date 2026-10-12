package com.bookingapi.domain.auth.service;

import com.bookingapi.domain.user.entity.User;

public interface RefreshTokenService {

    String issue(User user);

    RotatedToken rotate(String rawToken);

    void revoke(String rawToken);

    record RotatedToken(User user, String refreshToken) {
    }
}