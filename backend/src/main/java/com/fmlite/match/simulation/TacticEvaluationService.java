package com.fmlite.match.simulation;

import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.AttackStyle;
import com.fmlite.match.tactic.Formation;
import com.fmlite.match.tactic.LineHeight;
import com.fmlite.match.tactic.Mentality;
import com.fmlite.match.tactic.Pressing;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerTrait;
import org.springframework.stereotype.Service;

import java.util.List;

/** 내 전술 vs 상대 전술 상성 → 찬스 확률/찬스 질/중원 장악 보정치 */
@Service
public class TacticEvaluationService {

    public record Mods(double chanceMult, double qualityMult, double midMult, double oppMidMult) {}

    public Mods evaluate(TeamSimState me, TeamSimState opp, List<Player> myLineup) {
        double chance = 1.0, quality = 1.0, mid = 1.0, oppMid = 1.0;

        Mentality myMentality = me.getTactic().mentalityEnum();
        Pressing myPressing = me.getTactic().pressingEnum();
        AttackStyle myStyle = me.getTactic().attackStyleEnum();

        // 성향: 공격적일수록 찬스가 늘지만 리스크도 커진다 (상대 찬스 증가는 상대 계산에서 반영)
        if (myMentality == Mentality.ATTACKING) chance *= 1.15;
        if (myMentality == Mentality.DEFENSIVE) chance *= 0.85;
        // 상대 성향이 내 수비에 주는 영향
        if (opp.getTactic().mentalityEnum() == Mentality.ATTACKING) chance *= 1.10;
        if (opp.getTactic().mentalityEnum() == Mentality.DEFENSIVE) chance *= 0.90;

        // 강한 압박은 상대(=여기서는 나) 찬스를 줄인다
        if (opp.getTactic().pressingEnum() == Pressing.HIGH) chance *= 0.95;

        // 압박 HIGH vs 점유 스타일: 상대 중원 장악력 약화
        if (myPressing == Pressing.HIGH && opp.getTactic().attackStyleEnum() == AttackStyle.POSSESSION) {
            oppMid *= 0.92;
        }

        // 역습 vs 공격 성향: 찬스 질 상승
        if (myStyle == AttackStyle.COUNTER && opp.getTactic().mentalityEnum() == Mentality.ATTACKING) {
            quality *= 1.15;
        }

        // 측면 공격 vs 3백: 찬스 확률 상승
        if (myStyle == AttackStyle.WIDE && opp.getTactic().formationEnum() == Formation.F_3_5_2) {
            chance *= 1.10;
        }

        // 스타일 자체 보정
        switch (myStyle) {
            case POSSESSION -> { mid *= 1.05; chance *= 0.95; }
            case WIDE -> { chance *= 1.05; quality *= 0.95; }
            case COUNTER -> chance *= 0.95;
            case CENTER -> { /* 기준값 */ }
        }

        // 상대 높은 라인 + 침투 선호 보유: 찬스 확률 상승
        if (opp.getTactic().lineHeightEnum() == LineHeight.HIGH
                && myLineup.stream().anyMatch(p -> p.hasTrait(PlayerTrait.RUN_IN_BEHIND))) {
            chance *= 1.08;
        }

        // 상대 AI가 측면 수비를 보강한 경우
        if (opp.hasFlag(OpponentAiService.FLAG_WIDE_SHIELD) && myStyle == AttackStyle.WIDE) {
            chance *= 0.95;
        }

        return new Mods(chance, quality, mid, oppMid);
    }

    /** 압박 강도에 따른 체력 소모 배수 */
    public double staminaCostFactor(Pressing pressing) {
        return switch (pressing) {
            case LOW -> 0.7;
            case NORMAL -> 1.0;
            case HIGH -> 1.5;
        };
    }
}
