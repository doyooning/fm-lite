package com.fmlite.match.dto;

import com.fmlite.team.dto.TeamDetailResponse;

import java.util.List;

public record OpponentAnalysisResponse(
        OpponentTeam team,
        TeamDetailResponse.PowerByArea powerByArea,
        TeamDetailResponse.PowerByArea myPowerByArea,
        List<String> strengths,
        List<String> weaknesses,
        List<KeyPlayer> keyPlayers,
        TacticDto expectedTactic
) {
    public record OpponentTeam(Long id, String name, String shortName, String grade, String gradeLabel) {}

    public record KeyPlayer(Long id, String name, String position, int overall, String reason) {}
}
