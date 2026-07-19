package com.fmlite.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    @Column
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private User(UUID id, String email, String passwordHash, String nickname) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = (nickname == null || nickname.isBlank()) ? "감독" : nickname;
        this.emailVerified = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 이메일/비밀번호 계정 생성 (미인증 상태). email 은 소문자 정규화된 값이 들어온다. */
    public static User register(String email, String passwordHash, String nickname) {
        return new User(UUID.randomUUID(), email, passwordHash, nickname);
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }
}
