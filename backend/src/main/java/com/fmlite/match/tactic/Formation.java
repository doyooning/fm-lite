package com.fmlite.match.tactic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fmlite.common.exception.BusinessException;
import com.fmlite.player.Position;

import java.util.Map;

public enum Formation {
    F_4_3_3("4-3-3", Map.of(Position.GK, 1, Position.DF, 4, Position.MF, 3, Position.FW, 3)),
    F_4_2_3_1("4-2-3-1", Map.of(Position.GK, 1, Position.DF, 4, Position.MF, 5, Position.FW, 1)),
    F_3_5_2("3-5-2", Map.of(Position.GK, 1, Position.DF, 3, Position.MF, 5, Position.FW, 2));

    private final String value;
    private final Map<Position, Integer> slots;

    Formation(String value, Map<Position, Integer> slots) {
        this.value = value;
        this.slots = slots;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public Map<Position, Integer> getSlots() {
        return slots;
    }

    @JsonCreator
    public static Formation fromValue(String value) {
        for (Formation f : values()) {
            if (f.value.equals(value)) return f;
        }
        throw BusinessException.badRequest("INVALID_FORMATION", "지원하지 않는 포메이션입니다: " + value);
    }
}
