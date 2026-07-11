package com.fmlite.match.simulation.model;

import com.fmlite.match.event.ChoiceOption;
import com.fmlite.match.event.MatchEventType;

import java.util.List;

/** 엔진이 생성하는 이벤트 초안 (DB 저장 전) */
public record EventDraft(
        int minute,
        MatchEventType type,
        Long teamId,
        Long playerId,
        String description,
        boolean requiresChoice,
        List<ChoiceOption> choiceOptions
) {
    public static EventDraft simple(int minute, MatchEventType type, Long teamId, Long playerId, String description) {
        return new EventDraft(minute, type, teamId, playerId, description, false, null);
    }

    public static EventDraft info(int minute, MatchEventType type, String description) {
        return new EventDraft(minute, type, null, null, description, false, null);
    }

    public static EventDraft choice(int minute, String question, List<ChoiceOption> options) {
        return new EventDraft(minute, MatchEventType.CHOICE, null, null, question, true, options);
    }
}
