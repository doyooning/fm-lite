package com.fmlite.match.simulation;

import com.fmlite.match.Match;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.match.simulation.model.TacticSnapshot;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.Tactic;
import com.fmlite.player.Player;
import com.fmlite.team.Team;
import com.fmlite.player.PlayerRepository;
import com.fmlite.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.fmlite.match.simulation.SimulationConstants.CONDITION_MAX;
import static com.fmlite.match.simulation.SimulationConstants.CONDITION_MIN;

/** 시뮬레이션 준비 유틸: 스쿼드/팀명 로딩, 초기 상태 생성 */
@Component
@RequiredArgsConstructor
public class MatchSupport {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final LineupSelector lineupSelector;

    public Map<Long, List<Player>> squads(Match match) {
        Map<Long, List<Player>> squads = new HashMap<>();
        squads.put(match.getHomeTeamId(), playerRepository.findByTeamId(match.getHomeTeamId()));
        squads.put(match.getAwayTeamId(), playerRepository.findByTeamId(match.getAwayTeamId()));
        return squads;
    }

    public Map<Long, String> teamNames(Match match) {
        Map<Long, String> names = new HashMap<>();
        for (Long id : List.of(match.getHomeTeamId(), match.getAwayTeamId())) {
            names.put(id, teamRepository.findById(id).map(Team::getName).orElse("팀 " + id));
        }
        return names;
    }

    /**
     * 경기 시작 시 상태 초기화: 시드 생성, 라인업 자동 선발, 선수 컨디션(0.85~1.10) 부여.
     * @param userTeamId 사용자 조작 팀 (AI vs AI 경기는 null)
     */
    public SimulationState initState(Match match, Map<Long, Tactic> tactics, Long userTeamId,
                                     Map<Long, List<Player>> squads) {
        SimulationState state = new SimulationState();
        long seed = new Random().nextLong();
        state.setSeed(seed);
        Random conditionRng = new Random(seed);
        state.setHome(teamState(match.getHomeTeamId(), userTeamId, tactics, squads, conditionRng));
        state.setAway(teamState(match.getAwayTeamId(), userTeamId, tactics, squads, conditionRng));
        return state;
    }

    private TeamSimState teamState(Long teamId, Long userTeamId, Map<Long, Tactic> tactics,
                                   Map<Long, List<Player>> squads, Random rng) {
        TeamSimState ts = new TeamSimState();
        ts.setTeamId(teamId);
        ts.setAiControlled(!teamId.equals(userTeamId));
        Tactic tactic = tactics.get(teamId);
        ts.setTactic(TacticSnapshot.from(tactic));
        List<Player> squad = squads.get(teamId);
        ts.setLineup(lineupSelector.select(squad, tactic.getFormation()));
        for (Long playerId : ts.getLineup()) {
            double condition = CONDITION_MIN + rng.nextDouble() * (CONDITION_MAX - CONDITION_MIN);
            ts.getCondition().put(playerId, Math.round(condition * 100) / 100.0);
        }
        return ts;
    }
}
