package com.fmlite.match.simulation;

import com.fmlite.match.Match;
import com.fmlite.match.dto.MatchEventResponse;
import com.fmlite.match.event.MatchEvent;
import com.fmlite.match.event.MatchEventRepository;
import com.fmlite.match.simulation.model.EventDraft;
import com.fmlite.match.simulation.model.SimulationState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 엔진이 만든 EventDraft를 seq 부여 후 DB에 저장 */
@Component
@RequiredArgsConstructor
public class MatchEventWriter {

    private final MatchEventRepository matchEventRepository;

    public List<MatchEventResponse> persist(Match match, SimulationState state, List<EventDraft> drafts) {
        List<MatchEventResponse> responses = new ArrayList<>();
        for (EventDraft draft : drafts) {
            MatchEvent event = new MatchEvent(match.getId(), state.nextSeq(), draft.minute(),
                    draft.type(), draft.teamId(), draft.playerId(), draft.description(),
                    draft.requiresChoice(), draft.choiceOptions());
            responses.add(MatchEventResponse.from(matchEventRepository.save(event)));
        }
        return responses;
    }
}
