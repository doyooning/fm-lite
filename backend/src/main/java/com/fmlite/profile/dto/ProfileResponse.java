package com.fmlite.profile.dto;

import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        String email,
        Instant joinedAt,
        int championships,
        GameStats games,
        List<AchievementItem> achievements
) {
    public record GameStats(int total, int inProgress, int champion, int eliminated) {}

    public record AchievementItem(
            String code,
            String label,
            String description,
            String icon,
            boolean achieved,
            Instant achievedAt
    ) {}
}
