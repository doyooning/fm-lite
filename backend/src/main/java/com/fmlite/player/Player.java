package com.fmlite.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 시뮬레이션 단위 테스트에서 가상 선수 생성용
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Column(name = "back_number", nullable = false)
    private int backNumber;

    private int attack;
    private int defense;
    private int passing;
    private int speed;
    private int stamina;
    private int mentality;
    private int finishing;
    private int goalkeeping;

    @Column(columnDefinition = "text[]")
    private String[] traits;

    public List<PlayerTrait> traitList() {
        if (traits == null) return List.of();
        return Arrays.stream(traits).map(PlayerTrait::valueOf).toList();
    }

    public boolean hasTrait(PlayerTrait trait) {
        return traitList().contains(trait);
    }

    /** 포지션 가중 종합 능력치 (라인업 선발/표시용) */
    public int overall() {
        return switch (position) {
            case GK -> Math.round(goalkeeping * 0.7f + mentality * 0.2f + speed * 0.1f);
            case DF -> Math.round(defense * 0.5f + speed * 0.2f + mentality * 0.2f + passing * 0.1f);
            case MF -> Math.round(passing * 0.4f + stamina * 0.2f + attack * 0.2f + defense * 0.2f);
            case FW -> Math.round(finishing * 0.4f + attack * 0.3f + speed * 0.2f + mentality * 0.1f);
        };
    }
}
