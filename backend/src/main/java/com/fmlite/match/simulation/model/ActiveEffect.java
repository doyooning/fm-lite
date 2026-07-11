package com.fmlite.match.simulation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사용자 선택지의 지속 효과 — simulation_state jsonb에 저장 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveEffect {

    private String choiceId;
    private String side;          // "HOME" | "AWAY" (선택한 팀 기준)
    private int remainingTicks;

    public boolean isFor(boolean home) {
        return ("HOME".equals(side)) == home;
    }
}
