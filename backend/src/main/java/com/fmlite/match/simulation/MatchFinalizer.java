package com.fmlite.match.simulation;

import com.fmlite.match.Match;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.dto.MatchEventResponse;
import com.fmlite.match.result.MatchResult;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.match.simulation.MatchResultCalculator.FinalOutcome;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 정규시간 종료 후 마무리: 승부차기/통계/결과 저장 + 경기 FINISHED 처리 */
@Component
@RequiredArgsConstructor
public class MatchFinalizer {

    private final MatchResultCalculator resultCalculator;
    private final MatchResultRepository matchResultRepository;
    private final MatchEventWriter eventWriter;

    public List<MatchEventResponse> finalizeMatch(Match match, SimulationState state,
                                                  Map<Long, List<Player>> squads,
                                                  Map<Long, String> teamNames) {
        FinalOutcome outcome = resultCalculator.finalize(state, squads, teamNames);
        List<MatchEventResponse> events = eventWriter.persist(match, state, outcome.extraEvents());

        matchResultRepository.save(new MatchResult(
                match.getId(), state.getHomeScore(), state.getAwayScore(),
                outcome.penaltyHome(), outcome.penaltyAway(),
                outcome.winnerTeamId(), outcome.stats()));

        match.updateState(state, MatchStatus.FINISHED);
        return events;
    }
}
