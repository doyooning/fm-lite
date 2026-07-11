package com.fmlite.team.dto;

import com.fmlite.team.Team;

public record TeamSummaryResponse(
        Long id,
        String name,
        String shortName,
        String grade,
        String gradeLabel,
        String description,
        int avgRating
) {
    public static TeamSummaryResponse of(Team team, int avgRating) {
        return new TeamSummaryResponse(team.getId(), team.getName(), team.getShortName(),
                team.getGrade().name(), team.getGrade().getLabel(), team.getDescription(), avgRating);
    }
}
