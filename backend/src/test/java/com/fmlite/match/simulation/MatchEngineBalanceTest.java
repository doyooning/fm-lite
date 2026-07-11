package com.fmlite.match.simulation;

import com.fmlite.competition.Round;
import com.fmlite.match.simulation.MatchEngine.EngineContext;
import com.fmlite.match.simulation.MatchEngine.RunOutcome;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.match.simulation.model.TacticSnapshot;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.Formation;
import com.fmlite.player.Player;
import com.fmlite.player.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 몬테카를로 밸런스 테스트 (순수 엔진 — Spring 컨텍스트/DB 불필요).
 * 목표: 평균 총득점 1.8~4.0, 강팀(base 78) vs 약팀(base 60) 결정승부 승률 65% 이상.
 */
class MatchEngineBalanceTest {

    private final LineupSelector lineupSelector = new LineupSelector();
    private final MatchEngine engine = new MatchEngine(
            new TeamPowerCalculator(),
            new TacticEvaluationService(),
            new OpponentAiService(),
            new MatchChoiceService(),
            new MatchEventGenerator(),
            lineupSelector
    );

    private static final int MATCHES = 1000;

    @Test
    void averageGoals_equalTeams_isReasonable() {
        int totalGoals = 0;
        for (int i = 0; i < MATCHES; i++) {
            SimulationState st = playAiMatch(70, 70, i);
            totalGoals += st.getHomeScore() + st.getAwayScore();
        }
        double avg = (double) totalGoals / MATCHES;
        assertTrue(avg >= 1.8 && avg <= 4.0,
                "경기당 평균 득점이 범위를 벗어남: " + avg);
    }

    @Test
    void strongTeam_beatsWeakTeam_mostOfTheTime() {
        int strongWins = 0, weakWins = 0, draws = 0;
        for (int i = 0; i < MATCHES; i++) {
            SimulationState st = playAiMatch(78, 60, i);
            if (st.getHomeScore() > st.getAwayScore()) strongWins++;
            else if (st.getHomeScore() < st.getAwayScore()) weakWins++;
            else draws++;
        }
        double decisiveWinRate = (double) strongWins / (strongWins + weakWins);
        assertTrue(decisiveWinRate >= 0.65,
                "강팀 결정승부 승률이 너무 낮음: " + decisiveWinRate
                        + " (승 " + strongWins + " / 패 " + weakWins + " / 무 " + draws + ")");
        assertTrue(decisiveWinRate <= 0.97,
                "강팀 승률이 지나치게 높음(랜덤성 부족): " + decisiveWinRate);
    }

    @Test
    void userMatch_pausesAtChoiceTick() {
        List<Player> home = squad(100, 1L, 70);
        List<Player> away = squad(200, 2L, 70);
        SimulationState st = initState(home, away, 42L);
        st.getHome().setAiControlled(false);   // 사용자 팀

        RunOutcome outcome = engine.run(context(home, away, st));

        assertTrue(outcome.waitingChoice(), "선택지 지점에서 멈춰야 함");
        assertEquals(6, st.getCurrentTick(), "첫 선택지는 30분(틱 6)");
        assertTrue(outcome.events().get(outcome.events().size() - 1).requiresChoice());
    }

    // ===== helpers =====

    private SimulationState playAiMatch(int homeBase, int awayBase, int seedIndex) {
        List<Player> home = squad(1000 + seedIndex * 40, 1L, homeBase);
        List<Player> away = squad(3000 + seedIndex * 40, 2L, awayBase);
        SimulationState st = initState(home, away, seedIndex * 7727L + 13);
        RunOutcome outcome = engine.run(context(home, away, st));
        assertTrue(outcome.regularTimeFinished());
        return st;
    }

    private EngineContext context(List<Player> home, List<Player> away, SimulationState st) {
        return new EngineContext(Round.QF,
                Map.of(1L, home, 2L, away),
                Map.of(1L, "홈팀", 2L, "원정팀"),
                st);
    }

    private SimulationState initState(List<Player> home, List<Player> away, long seed) {
        SimulationState st = new SimulationState();
        st.setSeed(seed);
        st.setHome(teamState(1L, home, seed));
        st.setAway(teamState(2L, away, seed + 1));
        return st;
    }

    private TeamSimState teamState(long teamId, List<Player> squad, long seed) {
        TeamSimState ts = new TeamSimState();
        ts.setTeamId(teamId);
        ts.setAiControlled(true);
        ts.setTactic(new TacticSnapshot("4-3-3", "BALANCED", "NORMAL", "NORMAL", "CENTER"));
        ts.setLineup(lineupSelector.select(squad, Formation.F_4_3_3));
        Random rng = new Random(seed);
        for (Long id : ts.getLineup()) {
            ts.getCondition().put(id, 0.85 + rng.nextDouble() * 0.25);
        }
        return ts;
    }

    /** GK2 DF6 MF6 FW4 스쿼드 생성 — 능력치는 base ± 4 */
    private List<Player> squad(long idStart, long teamId, int base) {
        Random rng = new Random(idStart);
        List<Player> players = new ArrayList<>();
        long id = idStart;
        int backNumber = 1;
        for (Position pos : List.of(Position.GK, Position.GK,
                Position.DF, Position.DF, Position.DF, Position.DF, Position.DF, Position.DF,
                Position.MF, Position.MF, Position.MF, Position.MF, Position.MF, Position.MF,
                Position.FW, Position.FW, Position.FW, Position.FW)) {
            players.add(new Player(id++, teamId, "선수" + id, pos, backNumber++,
                    stat(base, rng), stat(base, rng), stat(base, rng), stat(base, rng),
                    stat(base, rng), stat(base, rng), stat(base, rng),
                    pos == Position.GK ? stat(base + 10, rng) : 10,
                    new String[0]));
        }
        return players;
    }

    private int stat(int base, Random rng) {
        return Math.max(1, Math.min(99, base + rng.nextInt(9) - 4));
    }
}
