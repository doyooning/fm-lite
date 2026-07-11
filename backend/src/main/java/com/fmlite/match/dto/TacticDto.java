package com.fmlite.match.dto;

import com.fmlite.match.simulation.model.TacticSnapshot;
import com.fmlite.match.tactic.Tactic;

public record TacticDto(
        String formation,
        String mentality,
        String pressing,
        String lineHeight,
        String attackStyle
) {
    public static TacticDto from(Tactic t) {
        return new TacticDto(t.getFormation().getValue(), t.getMentality().name(),
                t.getPressing().name(), t.getLineHeight().name(), t.getAttackStyle().name());
    }

    public static TacticDto from(TacticSnapshot s) {
        return new TacticDto(s.getFormation(), s.getMentality(), s.getPressing(),
                s.getLineHeight(), s.getAttackStyle());
    }
}
