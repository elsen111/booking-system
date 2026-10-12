package com.bookingapi.domain.auth.service;

import com.bookingapi.domain.auth.dto.LoginRequest;
import com.bookingapi.domain.auth.dto.RefreshTokenRequest;
import com.bookingapi.domain.auth.dto.RegisterRequest;
import com.bookingapi.domain.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}