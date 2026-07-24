package com.fmlite.profile;

import lombok.Getter;

/** 업적 정의 (코드는 user_achievements.code 로 저장) */
@Getter
public enum Achievement {
    FIRST_GAME("첫 발을 내딛다", "첫 게임을 생성했습니다.", "🎬"),
    FIRST_WIN("첫 우승", "FM 챔피언스 컵에서 처음으로 우승했습니다.", "🏆"),
    UNDERDOG_WIN("언더독의 반란", "중위팀 이하 전력으로 우승했습니다.", "🐺"),
    NO_CONCEDE("철벽 우승", "한 골도 내주지 않고 우승했습니다.", "🧱"),
    VETERAN("백전노장", "통산 3회 우승을 달성했습니다.", "🎖️");

    private final String label;
    private final String description;
    private final String icon;

    Achievement(String label, String description, String icon) {
        this.label = label;
        this.description = description;
        this.icon = icon;
    }
}
