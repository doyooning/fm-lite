package com.fmlite.savegame;

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
@Table(name = "save_games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaveGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** 이 저장 게임에서 사용할 감독 이름 */
    @Column(name = "manager_name", nullable = false)
    private String managerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaveGameStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SaveGame(UUID userId, Long teamId, String managerName) {
        this.userId = userId;
        this.teamId = teamId;
        this.managerName = (managerName == null || managerName.isBlank())
                ? "감독" : managerName.trim();
        this.status = SaveGameStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void finish(SaveGameStatus finalStatus) {
        this.status = finalStatus;
        this.updatedAt = Instant.now();
    }
}
