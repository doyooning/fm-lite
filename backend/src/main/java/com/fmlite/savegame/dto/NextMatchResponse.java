package com.fmlite.savegame.dto;

import com.fmlite.match.dto.TeamBrief;

public record NextMatchResponse(
        String saveGameStatus,
        boolean hasNext,
        NextMatch match
) {
    public record NextMatch(
            Long matchId,
            String round,
            String roundLabel,
            String status,
            TeamBrief homeTeam,
            TeamBrief awayTeam,
            boolean isUserHome,
            boolean tacticSubmitted
    ) {}
}
