package com.bookingapi.security.jwt;

import com.bookingapi.security.AuthenticatedUser;
import com.bookingapi.domain.user.entity.User;

public interface JwtService {

    String generateAccessToken(User user);

    AuthenticatedUser parseAccessToken(String token);
}