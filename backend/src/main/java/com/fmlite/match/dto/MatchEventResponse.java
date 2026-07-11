package com.fmlite.match.dto;

import com.fmlite.match.event.ChoiceOption;
import com.fmlite.match.event.MatchEvent;

import java.util.List;

public record MatchEventResponse(
        Long eventId,
        int seq,
        int minute,
        String eventType,
        Long teamId,
        Long playerId,
        String description,
        boolean requiresChoice,
        List<ChoiceOption> choiceOptions,
        String selectedChoiceId
) {
    public static MatchEventResponse from(MatchEvent e) {
        return new MatchEventResponse(e.getId(), e.getSeq(), e.getMinute(), e.getEventType().name(),
                e.getTeamId(), e.getPlayerId(), e.getDescription(), e.isRequiresChoice(),
                e.getChoiceOptions(), e.getSelectedChoiceId());
    }
}
