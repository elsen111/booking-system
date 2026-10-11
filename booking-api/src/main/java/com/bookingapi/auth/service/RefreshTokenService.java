package com.bookingapi.auth.service;

import com.bookingapi.user.domain.User;

public interface RefreshTokenService {

    String issue(User user);

    RotatedToken rotate(String rawToken);

    void revoke(String rawToken);

    record RotatedToken(User user, String refreshToken) {
    }
}