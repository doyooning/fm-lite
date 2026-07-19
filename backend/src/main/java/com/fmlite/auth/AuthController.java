package com.fmlite.auth;

import com.fmlite.auth.dto.AuthResponse;
import com.fmlite.auth.dto.LoginRequest;
import com.fmlite.auth.dto.RegisterRequest;
import com.fmlite.auth.dto.ResendRequest;
import com.fmlite.auth.dto.VerifyRequest;
import com.fmlite.common.response.ApiResponse;
import com.fmlite.security.CurrentUserId;
import com.fmlite.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(Map.of("message", "인증 메일을 발송했습니다. 메일함을 확인해 주세요."));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, String>> verify(@Valid @RequestBody VerifyRequest request) {
        authService.verify(request.token());
        return ApiResponse.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Map<String, String>> resend(@Valid @RequestBody ResendRequest request) {
        authService.resend(request.email());
        return ApiResponse.ok(Map.of("message", "인증 메일을 다시 발송했습니다."));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@CurrentUserId UUID userId) {
        return ApiResponse.ok(authService.me(userId));
    }
}
