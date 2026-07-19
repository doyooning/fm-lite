package com.fmlite.auth;

import com.fmlite.auth.dto.AuthResponse;
import com.fmlite.auth.dto.LoginRequest;
import com.fmlite.auth.dto.RegisterRequest;
import com.fmlite.common.exception.BusinessException;
import com.fmlite.security.JwtService;
import com.fmlite.user.User;
import com.fmlite.user.UserRepository;
import com.fmlite.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.verification.expiry-hours}")
    private long verificationExpiryHours;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 회원가입: 미인증 계정 생성 + 인증 메일 발송. 이메일 중복이면 409. */
    @Transactional
    public void register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "이미 가입된 이메일입니다.");
        }
        User user = userRepository.save(User.register(
                email, passwordEncoder.encode(request.password()), request.nickname()));
        issueAndSendToken(user);
    }

    /** 로그인: 자격 검증 + 이메일 인증 확인 후 JWT 발급. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED", "이메일 인증이 필요합니다. 메일함을 확인해 주세요.");
        }
        String token = jwtService.issue(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    /** 이메일 인증 토큰 처리 */
    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> invalidToken());
        if (!token.isUsable()) {
            throw invalidToken();
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> invalidToken());
        token.markUsed();
        user.verifyEmail();
    }

    /** 인증 메일 재발송. 사용자 존재 여부는 노출하지 않는다(이미 인증됐거나 없으면 조용히 성공). */
    @Transactional
    public void resend(String rawEmail) {
        userRepository.findByEmailIgnoreCase(normalize(rawEmail))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(this::issueAndSendToken);
    }

    @Transactional(readOnly = true)
    public UserResponse me(java.util.UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED", "로그인이 필요합니다."));
    }

    private void issueAndSendToken(User user) {
        String rawToken = generateToken();
        tokenRepository.save(new EmailVerificationToken(user.getId(), rawToken,
                Instant.now().plus(verificationExpiryHours, ChronoUnit.HOURS)));
        emailService.sendVerification(user.getEmail(), rawToken);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private BusinessException invalidToken() {
        return BusinessException.badRequest("INVALID_OR_EXPIRED_TOKEN",
                "유효하지 않거나 만료된 인증 링크입니다. 인증 메일을 다시 요청해 주세요.");
    }
}
