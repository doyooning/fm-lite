package com.fmlite.competition.dto;

import com.fmlite.match.dto.TeamBrief;

import java.util.List;

public record BracketResponse(
        Long id,
        String name,
        String currentRound,
        String currentRoundLabel,
        Long winnerTeamId,
        List<RoundBracket> rounds
) {
    public record RoundBracket(String round, String roundLabel, List<BracketMatch> matches) {}

    public record BracketMatch(
            Long matchId,
            int matchNo,
            TeamBrief homeTeam,
            TeamBrief awayTeam,
            String status,
            boolean isUserMatch,
            Integer homeScore,
            Integer awayScore,
            Integer penaltyHomeScore,
            Integer penaltyAwayScore,
            Long winnerTeamId
    ) {}
}
