package com.bookingapi.security.jwt;

import com.bookingapi.security.AuthenticatedUser;
import com.bookingapi.user.domain.User;

public interface JwtService {

    String generateAccessToken(User user);

    AuthenticatedUser parseAccessToken(String token);
}