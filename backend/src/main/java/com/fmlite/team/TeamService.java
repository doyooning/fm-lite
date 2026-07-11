package com.fmlite.team;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerRepository;
import com.fmlite.player.dto.PlayerResponse;
import com.fmlite.team.dto.TeamDetailResponse;
import com.fmlite.team.dto.TeamSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public List<TeamSummaryResponse> listTeams() {
        return teamRepository.findAll().stream()
                .map(team -> TeamSummaryResponse.of(team, avgRating(team.getId())))
                .toList();
    }

    public TeamDetailResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("팀"));
        List<Player> players = playerRepository.findByTeamId(teamId);
        return TeamDetailResponse.of(team, players);
    }

    public List<PlayerResponse> listPlayers(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw BusinessException.notFound("팀");
        }
        return playerRepository.findByTeamIdOrderByPositionAscBackNumberAsc(teamId).stream()
                .map(PlayerResponse::from)
                .toList();
    }

    private int avgRating(Long teamId) {
        List<Player> players = playerRepository.findByTeamId(teamId);
        return (int) Math.round(players.stream().mapToInt(Player::overall).average().orElse(0));
    }
}
