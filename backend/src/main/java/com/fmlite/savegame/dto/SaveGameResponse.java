package com.fmlite.savegame.dto;

import com.fmlite.competition.Competition;
import com.fmlite.match.dto.TeamBrief;
import com.fmlite.savegame.SaveGame;
import com.fmlite.team.Team;

import java.time.Instant;

public record SaveGameResponse(
        Long id,
        TeamBrief team,
        String teamGrade,
        String status,
        Long competitionId,
        String currentRound,
        String currentRoundLabel,
        Long winnerTeamId,
        Instant createdAt
) {
    public static SaveGameResponse of(SaveGame saveGame, Team team, Competition competition) {
        return new SaveGameResponse(
                saveGame.getId(),
                TeamBrief.from(team),
                team.getGrade().name(),
                saveGame.getStatus().name(),
                competition.getId(),
                competition.getCurrentRound().name(),
                competition.getCurrentRound().getLabel(),
                competition.getWinnerTeamId(),
                saveGame.getCreatedAt());
    }
}
