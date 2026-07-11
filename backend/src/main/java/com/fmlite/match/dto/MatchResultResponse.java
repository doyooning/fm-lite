package com.fmlite.match.dto;

import com.fmlite.match.result.MatchStats;

public record MatchResultResponse(
        Long matchId,
        String round,
        String roundLabel,
        TeamBrief homeTeam,
        TeamBrief awayTeam,
        int homeScore,
        int awayScore,
        Integer penaltyHomeScore,
        Integer penaltyAwayScore,
        Long winnerTeamId,
        Boolean userWon,
        String saveGameStatus,
        MatchStats stats
) {}
