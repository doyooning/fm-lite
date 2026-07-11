package com.fmlite.match.simulation;

/** 시뮬레이션 밸런스 상수 — 몬테카를로 테스트로 튜닝하는 값들을 한곳에 모은다 */
public final class SimulationConstants {

    private SimulationConstants() {}

    public static final int TOTAL_TICKS = 18;              // 90분 / 5분
    public static final int[] CHOICE_TICKS = {6, 11, 15};  // 30' / 55' / 75'

    public static final double BASE_CHANCE_PROB = 0.44;    // 틱당 찬스 발생 기본 확률 (지배력 곱 전)
    public static final double BASE_GOAL_PROB = 0.30;      // 찬스당 골 기본 확률 (전력비 곱 전)
    public static final double MIN_GOAL_PROB = 0.05;
    public static final double MAX_GOAL_PROB = 0.65;

    public static final double STAMINA_DROP_PER_TICK = 0.010;
    public static final double STAMINA_SOFT_FLOOR = 0.8;   // 이 아래로 떨어지면 전력 감소 시작

    public static final double MOMENTUM_DOMINANCE_BONUS = 0.03;  // 모멘텀 1당 지배력 보정

    public static final double CONDITION_MIN = 0.85;
    public static final double CONDITION_MAX = 1.10;
}
