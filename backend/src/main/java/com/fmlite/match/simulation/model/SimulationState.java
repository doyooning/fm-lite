package com.fmlite.match.simulation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 경기 진행 상태 스냅샷 — matches.simulation_state(jsonb)에 저장, 선택지 대기 후 재개용 */
@Getter
@Setter
@NoArgsConstructor
public class SimulationState {

    private long seed;
    private int currentTick = 0;          // 0~17 (틱당 5분), 18이면 정규시간 종료
    private int homeScore = 0;
    private int awayScore = 0;
    private int seqCounter = 0;           // 다음 이벤트 seq
    private boolean waitingChoice = false;
    private boolean halftimePrompted = false;   // 하프타임 전술변경 안내를 이미 띄웠는지
    private TeamSimState home;
    private TeamSimState away;
    private double domSumHome = 0;        // 점유율 계산용 (틱별 홈 지배력 합)
    private List<Integer> promptedChoiceTicks = new ArrayList<>();  // 재개 시 같은 틱 중복 프롬프트 방지
    private List<ActiveEffect> effects = new ArrayList<>();
    private List<GoalRecord> goals = new ArrayList<>();

    public int nextSeq() {
        return ++seqCounter;
    }

    public TeamSimState side(boolean isHome) {
        return isHome ? home : away;
    }

    public int scoreOf(boolean isHome) {
        return isHome ? homeScore : awayScore;
    }

    public void addGoal(boolean isHome) {
        if (isHome) homeScore++; else awayScore++;
    }

    public int minuteNow() {
        return Math.min(90, currentTick * 5);
    }
}
