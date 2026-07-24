package com.fmlite.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_achievements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Achievement code;

    @Column(name = "achieved_at", nullable = false)
    private Instant achievedAt;

    public UserAchievement(UUID userId, Achievement code) {
        this.userId = userId;
        this.code = code;
        this.achievedAt = Instant.now();
    }
}
