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

    /**
     * 슬롯 순서대로의 역할 목록 (GK → DF → MF → FW). 라인업의 i번째 선수는 i번째 슬롯 역할을 수행한다.
     * 선수의 본래 포지션과 무관하게 어느 슬롯에도 배치할 수 있다.
     */
    public java.util.List<Position> slotRoles() {
        java.util.List<Position> roles = new java.util.ArrayList<>();
        for (Position p : new Position[]{Position.GK, Position.DF, Position.MF, Position.FW}) {
            for (int i = 0; i < slots.getOrDefault(p, 0); i++) roles.add(p);
        }
        return roles;
    }

    @JsonCreator
    public static Formation fromValue(String value) {
        for (Formation f : values()) {
            if (f.value.equals(value)) return f;
        }
        throw BusinessException.badRequest("INVALID_FORMATION", "지원하지 않는 포메이션입니다: " + value);
    }
}
