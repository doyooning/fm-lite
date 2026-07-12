package com.fmlite.match.simulation;

import com.fmlite.match.Match;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.simulation.MatchEngine.EngineContext;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.match.simulation.model.TacticSnapshot;
import com.fmlite.match.tactic.Tactic;
import com.fmlite.match.tactic.TacticRepository;
import com.fmlite.player.Player;
import com.fmlite.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** AI vs AI 경기를 선택지 없이 즉시 완주 시뮬레이션 (토너먼트 자동 진행용) */
@Component
@RequiredArgsConstructor
public class AiMatchRunner {

    private final MatchEngine engine;
    private final MatchSupport matchSupport;
    private final MatchFinalizer finalizer;
    private final TacticRepository tacticRepository;
    private final TeamRepository teamRepository;
    private final OpponentAiService opponentAi;
    private final MatchEventWriter eventWriter;

    public void simulate(Match match) {
        if (match.getStatus() == MatchStatus.FINISHED) return;

        Map<Long, List<Player>> squads = matchSupport.squads(match);
        Map<Long, String> names = matchSupport.teamNames(match);

        Map<Long, Tactic> tactics = new HashMap<>();
        for (Long teamId : List.of(match.getHomeTeamId(), match.getAwayTeamId())) {
            tactics.put(teamId, tacticRepository.findByMatchIdAndTeamId(match.getId(), teamId)
                    .orElseGet(() -> tacticRepository.save(buildAiTactic(match.getId(), teamId))));
        }

        SimulationState state = matchSupport.initState(match, tactics, null, squads);
        match.updateState(state, MatchStatus.IN_PROGRESS);

        var outcome = engine.run(new EngineContext(match.getRound(), squads, names, state));
        eventWriter.persist(match, state, outcome.events());
        finalizer.finalizeMatch(match, state, squads, names);
    }

    public Tactic buildAiTactic(Long matchId, Long teamId) {
        TacticSnapshot snap = opponentAi.initialTactic(
                teamRepository.findById(teamId).orElseThrow());
        return new Tactic(matchId, teamId, snap.formationEnum(), snap.mentalityEnum(),
                snap.pressingEnum(), snap.lineHeightEnum(), snap.attackStyleEnum(), null);
    }
}
