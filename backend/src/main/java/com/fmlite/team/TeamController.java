package com.fmlite.team;

import com.fmlite.common.response.ApiResponse;
import com.fmlite.player.dto.PlayerResponse;
import com.fmlite.team.dto.TeamDetailResponse;
import com.fmlite.team.dto.TeamSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ApiResponse<List<TeamSummaryResponse>> listTeams() {
        return ApiResponse.ok(teamService.listTeams());
    }

    @GetMapping("/{teamId}")
    public ApiResponse<TeamDetailResponse> getTeam(@PathVariable Long teamId) {
        return ApiResponse.ok(teamService.getTeam(teamId));
    }

    @GetMapping("/{teamId}/players")
    public ApiResponse<List<PlayerResponse>> listPlayers(@PathVariable Long teamId) {
        return ApiResponse.ok(teamService.listPlayers(teamId));
    }
}
