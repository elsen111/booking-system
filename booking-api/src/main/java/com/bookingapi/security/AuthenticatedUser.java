package com.bookingapi.security;

import com.bookingapi.domain.user.entity.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}