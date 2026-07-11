package com.fmlite.match.simulation;

import com.fmlite.match.event.MatchEventType;
import com.fmlite.match.result.MatchStats;
import com.fmlite.match.simulation.model.EventDraft;
import com.fmlite.match.simulation.model.GoalRecord;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerTrait;
import com.fmlite.player.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static com.fmlite.match.simulation.SimulationConstants.TOTAL_TICKS;

/** 정규시간 종료 후 처리: 승부차기(동점 시), 통계 집계, 승자 결정 */
@Component
@RequiredArgsConstructor
public class MatchResultCalculator {

    private final MatchEventGenerator eventGenerator;
    private final LineupSelector lineupSelector;

    public record FinalOutcome(
            Long winnerTeamId,
            Integer penaltyHome,
            Integer penaltyAway,
            MatchStats stats,
            List<EventDraft> extraEvents
    ) {}

    public FinalOutcome finalize(SimulationState st, Map<Long, List<Player>> squads,
                                 Map<Long, String> teamNames) {
        Random rng = new Random(st.getSeed() ^ 999_983L);
        List<EventDraft> extra = new java.util.ArrayList<>();

        extra.add(EventDraft.info(90, MatchEventType.FULL_TIME,
                eventGenerator.fullTime(st.getHomeScore(), st.getAwayScore())));

        Long homeId = st.getHome().getTeamId();
        Long awayId = st.getAway().getTeamId();
        Long winner;
        Integer penHome = null, penAway = null;

        if (st.getHomeScore() > st.getAwayScore()) {
            winner = homeId;
        } else if (st.getHomeScore() < st.getAwayScore()) {
            winner = awayId;
        } else {
            extra.add(EventDraft.info(90, MatchEventType.PENALTY_SHOOTOUT, eventGenerator.shootoutStart()));
            int[] pens = shootout(st, squads, rng);
            penHome = pens[0];
            penAway = pens[1];
            winner = penHome > penAway ? homeId : awayId;
            extra.add(EventDraft.info(90, MatchEventType.PENALTY_SHOOTOUT,
                    eventGenerator.shootoutResult(teamNames.get(winner), penHome, penAway)));
        }

        MatchStats stats = buildStats(st, squads, winner);
        return new FinalOutcome(winner, penHome, penAway, stats, extra);
    }

    /** 간이 승부차기: 5라운드 + 서든데스(최대 5라운드 추가) */
    private int[] shootout(SimulationState st, Map<Long, List<Player>> squads, Random rng) {
        List<Player> homeKickers = kickers(squads.get(st.getHome().getTeamId()), st.getHome().getLineup());
        List<Player> awayKickers = kickers(squads.get(st.getAway().getTeamId()), st.getAway().getLineup());
        Player homeGk = gk(squads.get(st.getHome().getTeamId()), st.getHome().getLineup());
        Player awayGk = gk(squads.get(st.getAway().getTeamId()), st.getAway().getLineup());

        int home = 0, away = 0;
        for (int round = 0; round < 10; round++) {
            Player hk = homeKickers.get(round % homeKickers.size());
            Player ak = awayKickers.get(round % awayKickers.size());
            if (rng.nextDouble() < kickSuccessProb(hk, awayGk)) home++;
            if (rng.nextDouble() < kickSuccessProb(ak, homeGk)) away++;
            if (round >= 4 && home != away) break;   // 5라운드 이후 승부 갈리면 종료
        }
        if (home == away) {   // 10라운드에도 동점이면 멘탈 합으로 결정 (극단 케이스 방어)
            boolean homeWins = mentalitySum(homeKickers) >= mentalitySum(awayKickers);
            if (homeWins) home++; else away++;
        }
        return new int[]{home, away};
    }

    private double kickSuccessProb(Player kicker, Player gk) {
        double p = 0.78
                + (kicker.getMentality() - 70) * 0.003
                - (gk.getGoalkeeping() - 70) * 0.004
                - (gk.hasTrait(PlayerTrait.PK_SAVER) ? 0.12 : 0);
        return Math.max(0.40, Math.min(0.95, p));
    }

    private List<Player> kickers(List<Player> squad, List<Long> lineup) {
        return lineupSelector.lineupPlayers(squad, lineup).stream()
                .filter(p -> p.getPosition() != Position.GK)
                .sorted(Comparator.comparingInt((Player p) -> p.getFinishing() + p.getMentality()).reversed())
                .limit(5)
                .toList();
    }

    private Player gk(List<Player> squad, List<Long> lineup) {
        return lineupSelector.lineupPlayers(squad, lineup).stream()
                .filter(p -> p.getPosition() == Position.GK)
                .findFirst().orElseThrow();
    }

    private int mentalitySum(List<Player> players) {
        return players.stream().mapToInt(Player::getMentality).sum();
    }

    private MatchStats buildStats(SimulationState st, Map<Long, List<Player>> squads, Long winnerTeamId) {
        int possessionHome = (int) Math.round(st.getDomSumHome() / TOTAL_TICKS * 100);
        Player best = bestPlayer(st, squads, winnerTeamId);
        return new MatchStats(
                possessionHome, 100 - possessionHome,
                st.getHome().getShots(), st.getAway().getShots(),
                st.getHome().getShotsOnTarget(), st.getAway().getShotsOnTarget(),
                best.getId(), best.getName()
        );
    }

    /** 베스트 플레이어: 다득점자 우선, 무득점 경기면 승리팀 최고 능력치 선수 */
    private Player bestPlayer(SimulationState st, Map<Long, List<Player>> squads, Long winnerTeamId) {
        Map<Long, Long> goalsByPlayer = st.getGoals().stream()
                .collect(Collectors.groupingBy(GoalRecord::getPlayerId, Collectors.counting()));
        if (!goalsByPlayer.isEmpty()) {
            Long topScorerId = goalsByPlayer.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow().getKey();
            return squads.values().stream().flatMap(List::stream)
                    .filter(p -> p.getId().equals(topScorerId))
                    .findFirst().orElseThrow();
        }
        List<Long> winnerLineup = st.getHome().getTeamId() == winnerTeamId
                ? st.getHome().getLineup() : st.getAway().getLineup();
        return lineupSelector.lineupPlayers(squads.get(winnerTeamId), winnerLineup).stream()
                .max(Comparator.comparingInt(Player::overall))
                .orElseThrow();
    }
}
