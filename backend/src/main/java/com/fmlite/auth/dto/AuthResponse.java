package com.fmlite.auth.dto;

import com.fmlite.user.dto.UserResponse;

public record AuthResponse(String token, UserResponse user) {}
