package com.fmlite.match.dto;

import com.fmlite.team.Team;

public record TeamBrief(Long id, String name, String shortName) {

    public static TeamBrief from(Team team) {
        return new TeamBrief(team.getId(), team.getName(), team.getShortName());
    }
}
