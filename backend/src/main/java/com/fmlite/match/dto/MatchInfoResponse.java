package com.fmlite.match.dto;

public record MatchInfoResponse(
        Long matchId,
        String round,
        String roundLabel,
        String status,
        TeamBrief homeTeam,
        TeamBrief awayTeam,
        boolean isUserMatch,
        boolean isUserHome,
        boolean tacticSubmitted,
        MatchProgressResponse.Score score
) {}
