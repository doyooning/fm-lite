package com.fmlite.match.simulation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 팀 하나의 경기 중 상태 — simulation_state jsonb에 저장 */
@Getter
@Setter
@NoArgsConstructor
public class TeamSimState {

    private long teamId;
    private boolean aiControlled;
    private TacticSnapshot tactic;
    private List<Long> lineup = new ArrayList<>();
    private Map<Long, Double> condition = new HashMap<>();
    private double stamina = 1.0;
    private int momentum = 0;
    private int shots = 0;
    private int shotsOnTarget = 0;
    private int wideChancesConceded = 0;
    private List<String> aiFlags = new ArrayList<>();

    public boolean hasFlag(String flag) {
        return aiFlags.contains(flag);
    }

    public void addFlag(String flag) {
        if (!aiFlags.contains(flag)) aiFlags.add(flag);
    }

    public void addMomentum(int delta) {
        momentum = Math.max(-3, Math.min(3, momentum + delta));
    }
}
