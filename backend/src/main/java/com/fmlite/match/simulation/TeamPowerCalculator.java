package com.fmlite.match.simulation;

import com.fmlite.competition.Round;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.Pressing;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerTrait;
import com.fmlite.player.Position;
import org.springframework.stereotype.Component;

import java.util.List;

/** 선발 XI + 컨디션 + 체력 + 특성 → 경기 중 지역별 전력 */
@Component
public class TeamPowerCalculator {

    public record Powers(double attack, double midfield, double defense, double gk) {}

    public Powers matchPowers(List<Player> lineup, TeamSimState team, TacticContext ctx) {
        double staminaFactor = staminaFactor(team.getStamina());

        double attack = 0, midfield = 0, defense = 0, gk = 0;
        int fw = 0, mf = 0, df = 0;

        for (Player p : lineup) {
            double cond = team.getCondition().getOrDefault(p.getId(), 1.0);
            double traitMult = 1.0;
            if (p.hasTrait(PlayerTrait.BIG_GAME_PLAYER) && ctx.round() != Round.QF) traitMult *= 1.05;
            if (p.hasTrait(PlayerTrait.LOW_STAMINA) && ctx.tick() >= 12) traitMult *= 0.90;
            double eff = cond * traitMult;

            switch (p.getPosition()) {
                case FW -> {
                    attack += (p.getAttack() * 0.4 + p.getFinishing() * 0.4 + p.getSpeed() * 0.2) * eff;
                    fw++;
                }
                case MF -> {
                    double passing = p.getPassing();
                    if (p.hasTrait(PlayerTrait.WEAK_UNDER_PRESSURE) && ctx.oppPressing() == Pressing.HIGH) {
                        passing *= 0.90;
                    }
                    double base = passing * 0.5 + p.getStamina() * 0.25 + p.getMentality() * 0.25;
                    if (p.hasTrait(PlayerTrait.PASS_MASTER)) base += 5;
                    midfield += base * eff;
                    mf++;
                }
                case DF -> {
                    double d = p.getDefense();
                    if (p.hasTrait(PlayerTrait.POOR_CONCENTRATION) && ctx.tick() >= 15) d *= 0.90;
                    defense += (d * 0.6 + p.getSpeed() * 0.2 + p.getMentality() * 0.2) * eff;
                    df++;
                }
                case GK -> gk = p.getGoalkeeping() * eff;
            }
        }

        return new Powers(
                safeAvg(attack, fw) * staminaFactor,
                safeAvg(midfield, mf) * staminaFactor,
                safeAvg(defense, df) * staminaFactor,
                gk * staminaFactor
        );
    }

    public record TacticContext(Round round, int tick, Pressing oppPressing) {}

    private double staminaFactor(double stamina) {
        if (stamina >= SimulationConstants.STAMINA_SOFT_FLOOR) return 1.0;
        return Math.max(0.5, 1.0 - (SimulationConstants.STAMINA_SOFT_FLOOR - stamina) * 1.5);
    }

    private double safeAvg(double sum, int count) {
        return count == 0 ? 1 : sum / count;
    }

    public Player goalkeeper(List<Player> lineup) {
        return lineup.stream().filter(p -> p.getPosition() == Position.GK).findFirst().orElse(lineup.get(0));
    }
}
