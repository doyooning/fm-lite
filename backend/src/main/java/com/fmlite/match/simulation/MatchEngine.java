package com.fmlite.match.simulation;

import com.fmlite.competition.Round;
import com.fmlite.match.event.MatchEventType;
import com.fmlite.match.simulation.MatchChoiceService.ChoicePrompt;
import com.fmlite.match.simulation.TeamPowerCalculator.Powers;
import com.fmlite.match.simulation.TeamPowerCalculator.TacticContext;
import com.fmlite.match.simulation.model.ActiveEffect;
import com.fmlite.match.simulation.model.EventDraft;
import com.fmlite.match.simulation.model.GoalRecord;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.AttackStyle;
import com.fmlite.match.tactic.LineHeight;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerTrait;
import com.fmlite.player.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.fmlite.match.simulation.SimulationConstants.*;

/**
 * 틱 기반 경기 시뮬레이션 엔진 (순수 로직 — DB/HTTP를 모른다).
 * 다음 선택지 지점 또는 정규시간 종료까지 진행하고 상태를 갱신한다.
 */
@Component
@RequiredArgsConstructor
public class MatchEngine {

    private final TeamPowerCalculator powerCalculator;
    private final TacticEvaluationService tacticEvaluation;
    private final OpponentAiService opponentAi;
    private final MatchChoiceService choiceService;
    private final MatchEventGenerator eventGenerator;
    private final LineupSelector lineupSelector;

    /** 엔진 입력: 경기 메타 + 양 팀 스쿼드 + 진행 상태 */
    public record EngineContext(
            Round round,
            Map<Long, List<Player>> squads,
            Map<Long, String> teamNames,
            SimulationState state
    ) {}

    public record RunOutcome(List<EventDraft> events, boolean regularTimeFinished, boolean waitingChoice) {}

    public RunOutcome run(EngineContext ctx) {
        SimulationState st = ctx.state();
        List<EventDraft> events = new ArrayList<>();
        boolean userMatch = !st.getHome().isAiControlled() || !st.getAway().isAiControlled();

        if (st.getCurrentTick() == 0 && st.getSeqCounter() == 0) {
            events.add(EventDraft.info(0, MatchEventType.KICK_OFF,
                    eventGenerator.kickOff(name(ctx, st.getHome()), name(ctx, st.getAway()))));
        }

        while (st.getCurrentTick() < TOTAL_TICKS) {
            int tick = st.getCurrentTick();

            // 선택지 지점: 해당 틱을 플레이하기 전에 멈춘다
            if (userMatch && isChoiceTick(tick) && !st.getPromptedChoiceTicks().contains(tick)) {
                st.getPromptedChoiceTicks().add(tick);
                st.setWaitingChoice(true);
                TeamSimState user = userSide(st);
                TeamSimState opp = user == st.getHome() ? st.getAway() : st.getHome();
                int myScore = st.scoreOf(user == st.getHome());
                int oppScore = st.scoreOf(user != st.getHome());
                ChoicePrompt prompt = choiceService.buildPrompt(myScore, oppScore, user.getMomentum(), tick * 5);
                events.add(EventDraft.choice(tick * 5, prompt.question(), prompt.options()));
                return new RunOutcome(events, false, true);
            }

            Random rng = new Random(st.getSeed() ^ (7919L * (tick + 1)));

            if (tick == 9) {
                events.add(EventDraft.info(45, MatchEventType.HALF_TIME,
                        eventGenerator.halfTime(st.getHomeScore(), st.getAwayScore())));
            }

            playTick(ctx, st, tick, rng, events);

            // 효과 지속시간 감소
            st.getEffects().forEach(e -> e.setRemainingTicks(e.getRemainingTicks() - 1));
            st.getEffects().removeIf(e -> e.getRemainingTicks() <= 0);

            st.setCurrentTick(tick + 1);
        }

        return new RunOutcome(events, true, false);
    }

    private void playTick(EngineContext ctx, SimulationState st, int tick, Random rng, List<EventDraft> events) {
        TeamSimState home = st.getHome();
        TeamSimState away = st.getAway();

        // 1. 상대 AI 전술 조정 (AI 팀만)
        adjustAi(ctx, st, home, away, tick, rng, events);
        adjustAi(ctx, st, away, home, tick, rng, events);

        // 2. 체력 감소 (압박 강도 비례)
        drainStamina(home);
        drainStamina(away);

        // 3. 전력/보정치 계산
        List<Player> homeLineup = lineupSelector.lineupPlayers(ctx.squads().get(home.getTeamId()), home.getLineup());
        List<Player> awayLineup = lineupSelector.lineupPlayers(ctx.squads().get(away.getTeamId()), away.getLineup());

        Powers homePowers = powerCalculator.matchPowers(homeLineup, home,
                new TacticContext(ctx.round(), tick, away.getTactic().pressingEnum()));
        Powers awayPowers = powerCalculator.matchPowers(awayLineup, away,
                new TacticContext(ctx.round(), tick, home.getTactic().pressingEnum()));

        var homeMods = tacticEvaluation.evaluate(home, away, homeLineup);
        var awayMods = tacticEvaluation.evaluate(away, home, awayLineup);

        // 4. 중원 지배력
        double homeMid = homePowers.midfield() * homeMods.midMult() * awayMods.oppMidMult()
                * (1 + MOMENTUM_DOMINANCE_BONUS * home.getMomentum());
        double awayMid = awayPowers.midfield() * awayMods.midMult() * homeMods.oppMidMult()
                * (1 + MOMENTUM_DOMINANCE_BONUS * away.getMomentum());
        double domHome = homeMid / (homeMid + awayMid);
        st.setDomSumHome(st.getDomSumHome() + domHome);

        // 5. 찬스 판정 (양 팀 독립 — 동시 발생 시 지배력 가중으로 한 팀만)
        double homeChanceProb = BASE_CHANCE_PROB * domHome * homeMods.chanceMult() * effectChanceMult(st, true);
        double awayChanceProb = BASE_CHANCE_PROB * (1 - domHome) * awayMods.chanceMult() * effectChanceMult(st, false);

        boolean homeChance = rng.nextDouble() < homeChanceProb;
        boolean awayChance = rng.nextDouble() < awayChanceProb;
        if (homeChance && awayChance) {
            boolean keepHome = rng.nextDouble() < domHome;
            homeChance = keepHome;
            awayChance = !keepHome;
        }

        if (homeChance) {
            resolveChance(ctx, st, tick, rng, events, true, homeLineup, awayLineup, awayPowers,
                    homeMods.qualityMult() * effectQualityMult(st, true));
        } else if (awayChance) {
            resolveChance(ctx, st, tick, rng, events, false, awayLineup, homeLineup, homePowers,
                    awayMods.qualityMult() * effectQualityMult(st, false));
        }
    }

    private void resolveChance(EngineContext ctx, SimulationState st, int tick, Random rng,
                               List<EventDraft> events, boolean isHome,
                               List<Player> attackers, List<Player> defenders,
                               Powers defPowers, double quality) {
        TeamSimState atkTeam = st.side(isHome);
        TeamSimState defTeam = st.side(!isHome);
        String teamName = name(ctx, atkTeam);
        int minute = Math.min(90, tick * 5 + 1 + rng.nextInt(5));

        // 측면 공격 허용 추적 (상대 AI의 측면 수비 보강 트리거)
        if (atkTeam.getTactic().attackStyleEnum() == AttackStyle.WIDE) {
            defTeam.setWideChancesConceded(defTeam.getWideChancesConceded() + 1);
        }

        Player attacker = pickAttacker(attackers, defTeam, rng);
        double cond = atkTeam.getCondition().getOrDefault(attacker.getId(), 1.0);
        double attackerScore = (attacker.getFinishing() * 0.6 + attacker.getAttack() * 0.4) * cond * quality;
        double defGkScore = defPowers.defense() * 0.5 + defPowers.gk() * 0.5;

        double goalProb = BASE_GOAL_PROB * (attackerScore / (attackerScore + defGkScore)) * 2;
        if (attacker.hasTrait(PlayerTrait.AERIAL_THREAT)
                && atkTeam.getTactic().attackStyleEnum() == AttackStyle.WIDE) {
            goalProb *= 1.10;
        }
        goalProb = Math.max(MIN_GOAL_PROB, Math.min(MAX_GOAL_PROB, goalProb));

        atkTeam.setShots(atkTeam.getShots() + 1);
        double roll = rng.nextDouble();

        if (roll < goalProb) {
            atkTeam.setShotsOnTarget(atkTeam.getShotsOnTarget() + 1);
            st.addGoal(isHome);
            st.getGoals().add(new GoalRecord(atkTeam.getTeamId(), attacker.getId(), minute));
            atkTeam.addMomentum(1);
            defTeam.addMomentum(-1);
            events.add(EventDraft.simple(minute, MatchEventType.GOAL, atkTeam.getTeamId(), attacker.getId(),
                    eventGenerator.goal(teamName, attacker.getName(), rng)
                            + " (" + st.getHomeScore() + " : " + st.getAwayScore() + ")"));
        } else if (roll < goalProb + (1 - goalProb) * 0.55) {
            atkTeam.setShotsOnTarget(atkTeam.getShotsOnTarget() + 1);
            Player gk = powerCalculator.goalkeeper(defenders);
            events.add(EventDraft.simple(minute, MatchEventType.SAVE, defTeam.getTeamId(), gk.getId(),
                    eventGenerator.save(teamName, attacker.getName(), gk.getName(), rng)));
        } else {
            events.add(EventDraft.simple(minute, MatchEventType.MISS, atkTeam.getTeamId(), attacker.getId(),
                    eventGenerator.miss(teamName, attacker.getName(), rng)));
        }
    }

    private Player pickAttacker(List<Player> lineup, TeamSimState defTeam, Random rng) {
        List<Player> candidates = lineup.stream()
                .filter(p -> p.getPosition() == Position.FW || p.getPosition() == Position.MF)
                .toList();
        double[] weights = new double[candidates.size()];
        double total = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Player p = candidates.get(i);
            double w = (p.getAttack() + p.getFinishing()) * (p.getPosition() == Position.FW ? 1.0 : 0.45);
            if (p.hasTrait(PlayerTrait.RUN_IN_BEHIND)
                    && defTeam.getTactic().lineHeightEnum() == LineHeight.HIGH) w *= 1.8;
            if (p.hasTrait(PlayerTrait.LONG_SHOT)) w *= 1.2;
            weights[i] = w;
            total += w;
        }
        double r = rng.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r <= 0) return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }

    private void adjustAi(EngineContext ctx, SimulationState st, TeamSimState me, TeamSimState opp,
                          int tick, Random rng, List<EventDraft> events) {
        if (!me.isAiControlled()) return;
        int myScore = st.scoreOf(me == st.getHome());
        int oppScore = st.scoreOf(me != st.getHome());
        for (String change : opponentAi.adjust(me, opp, myScore, oppScore, tick, rng)) {
            events.add(EventDraft.simple(Math.min(90, tick * 5), MatchEventType.TACTIC_CHANGE,
                    me.getTeamId(), null, change));
        }
    }

    private void drainStamina(TeamSimState team) {
        double cost = STAMINA_DROP_PER_TICK * tacticEvaluation.staminaCostFactor(team.getTactic().pressingEnum());
        team.setStamina(Math.max(0.4, team.getStamina() - cost));
    }

    private double effectChanceMult(SimulationState st, boolean forHome) {
        double mult = 1.0;
        for (ActiveEffect e : st.getEffects()) {
            var spec = choiceService.spec(e.getChoiceId());
            if (spec == null) continue;
            mult *= e.isFor(forHome) ? spec.myChanceMult() : spec.oppChanceMult();
        }
        return mult;
    }

    private double effectQualityMult(SimulationState st, boolean forHome) {
        double mult = 1.0;
        for (ActiveEffect e : st.getEffects()) {
            var spec = choiceService.spec(e.getChoiceId());
            if (spec == null) continue;
            mult *= e.isFor(forHome) ? spec.myQualityMult() : spec.oppQualityMult();
        }
        return mult;
    }

    private boolean isChoiceTick(int tick) {
        return Arrays.stream(CHOICE_TICKS).anyMatch(t -> t == tick);
    }

    private TeamSimState userSide(SimulationState st) {
        return st.getHome().isAiControlled() ? st.getAway() : st.getHome();
    }

    private String name(EngineContext ctx, TeamSimState team) {
        return ctx.teamNames().get(team.getTeamId());
    }
}
