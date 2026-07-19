package com.fmlite.user.dto;

import com.fmlite.user.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, String nickname, boolean emailVerified) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.isEmailVerified());
    }
}
