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

    @Column(nullable = false)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public User(String nickname) {
        this(UUID.randomUUID(), nickname);
    }

    /** 클라이언트가 보관한 익명 id 로 사용자를 생성한다 (DB 초기화 등으로 유실된 경우 자기치유용) */
    public User(UUID id, String nickname) {
        this.id = id;
        this.nickname = (nickname == null || nickname.isBlank()) ? "감독" : nickname;
        this.createdAt = Instant.now();
    }
}
