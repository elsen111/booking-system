package com.bookingapi.auth.service;

import com.bookingapi.auth.dto.LoginRequest;
import com.bookingapi.auth.dto.RefreshTokenRequest;
import com.bookingapi.auth.dto.RegisterRequest;
import com.bookingapi.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}