package com.fmlite.match.simulation;

import com.fmlite.match.simulation.model.TacticSnapshot;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.AttackStyle;
import com.fmlite.match.tactic.Formation;
import com.fmlite.match.tactic.LineHeight;
import com.fmlite.match.tactic.Mentality;
import com.fmlite.match.tactic.Pressing;
import com.fmlite.team.Team;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 상태 기반 규칙 AI: 상대 팀의 초기 전술 결정 + 경기 중 전술 조정 */
@Service
public class OpponentAiService {

    public static final String FLAG_CHASE = "CHASE";
    public static final String FLAG_PROTECT = "PROTECT";
    public static final String FLAG_TIRED = "TIRED";
    public static final String FLAG_COUNTER_SWITCH = "COUNTER_SWITCH";
    public static final String FLAG_WIDE_SHIELD = "WIDE_SHIELD";

    /** 팀 콘셉트(약칭) 기반 기본 전술 — seed 데이터의 8팀과 매칭, 그 외에는 등급 기본값 */
    public TacticSnapshot initialTactic(Team team) {
        return switch (team.getShortName()) {
            case "RSF" -> snapshot(Formation.F_4_3_3, Mentality.ATTACKING, Pressing.HIGH, LineHeight.HIGH, AttackStyle.CENTER);
            case "BLK" -> snapshot(Formation.F_4_2_3_1, Mentality.BALANCED, Pressing.NORMAL, LineHeight.NORMAL, AttackStyle.POSSESSION);
            case "GDE" -> snapshot(Formation.F_4_2_3_1, Mentality.BALANCED, Pressing.NORMAL, LineHeight.LOW, AttackStyle.COUNTER);
            case "SVW" -> snapshot(Formation.F_3_5_2, Mentality.BALANCED, Pressing.HIGH, LineHeight.NORMAL, AttackStyle.POSSESSION);
            case "GRF" -> snapshot(Formation.F_4_3_3, Mentality.BALANCED, Pressing.NORMAL, LineHeight.NORMAL, AttackStyle.CENTER);
            case "ORW" -> snapshot(Formation.F_4_3_3, Mentality.BALANCED, Pressing.NORMAL, LineHeight.NORMAL, AttackStyle.WIDE);
            case "PPG" -> snapshot(Formation.F_4_2_3_1, Mentality.DEFENSIVE, Pressing.LOW, LineHeight.LOW, AttackStyle.COUNTER);
            case "WFX" -> snapshot(Formation.F_3_5_2, Mentality.BALANCED, Pressing.NORMAL, LineHeight.NORMAL, AttackStyle.COUNTER);
            default -> switch (team.getGrade()) {
                case STRONG -> snapshot(Formation.F_4_3_3, Mentality.ATTACKING, Pressing.HIGH, LineHeight.HIGH, AttackStyle.CENTER);
                case UPPER_MID -> snapshot(Formation.F_4_2_3_1, Mentality.BALANCED, Pressing.NORMAL, LineHeight.NORMAL, AttackStyle.POSSESSION);
                case MID -> snapshot(Formation.F_4_3_3, Mentality.BALANCED, Pressing.NORMAL, LineHeight.NORMAL, AttackStyle.CENTER);
                case WEAK -> snapshot(Formation.F_4_2_3_1, Mentality.DEFENSIVE, Pressing.LOW, LineHeight.LOW, AttackStyle.COUNTER);
            };
        };
    }

    /**
     * 틱마다 호출되는 상태 기반 전술 조정. 규칙별로 경기당 1회만 발동한다.
     * @return 발동한 조정에 대한 중계 텍스트 목록 (없으면 빈 리스트)
     */
    public List<String> adjust(TeamSimState me, TeamSimState opp, int myScore, int oppScore,
                               int tick, Random rng) {
        List<String> changes = new ArrayList<>();
        TacticSnapshot t = me.getTactic();

        // 60분 이후 지고 있으면 공격 성향 증가
        if (tick >= 12 && myScore < oppScore && !me.hasFlag(FLAG_CHASE)) {
            me.addFlag(FLAG_CHASE);
            t.setMentality(Mentality.ATTACKING.name());
            t.setLineHeight(raise(t.lineHeightEnum()).name());
            changes.add("상대가 승부수를 띄웁니다! 라인을 올리고 공격적으로 전환합니다.");
        }

        // 70분 이후 이기고 있으면 수비 성향 증가
        if (tick >= 14 && myScore > oppScore && !me.hasFlag(FLAG_PROTECT)) {
            me.addFlag(FLAG_PROTECT);
            t.setMentality(Mentality.DEFENSIVE.name());
            t.setPressing(lower(t.pressingEnum()).name());
            changes.add("상대가 리드를 지키기 위해 수비적으로 내려섭니다.");
        }

        // 체력이 낮으면 압박 강도 감소
        if (me.getStamina() < 0.75 && !me.hasFlag(FLAG_TIRED)) {
            me.addFlag(FLAG_TIRED);
            t.setPressing(lower(t.pressingEnum()).name());
            changes.add("상대 선수들의 체력이 떨어져 압박 강도가 눈에 띄게 낮아집니다.");
        }

        // 사용자가 높은 라인을 쓰면 뒷공간 침투(역습) 전환 시도
        if (opp.getTactic().lineHeightEnum() == LineHeight.HIGH && tick >= 6
                && !me.hasFlag(FLAG_COUNTER_SWITCH) && rng.nextDouble() < 0.3) {
            me.addFlag(FLAG_COUNTER_SWITCH);
            t.setAttackStyle(AttackStyle.COUNTER.name());
            changes.add("상대가 우리 수비 뒷공간을 노리는 역습 전술로 전환합니다.");
        }

        // 측면 공격을 계속 허용하면 측면 수비 보강
        if (me.getWideChancesConceded() >= 2 && !me.hasFlag(FLAG_WIDE_SHIELD)) {
            me.addFlag(FLAG_WIDE_SHIELD);
            changes.add("상대가 측면 수비를 보강합니다.");
        }

        return changes;
    }

    private TacticSnapshot snapshot(Formation f, Mentality m, Pressing p, LineHeight l, AttackStyle a) {
        return new TacticSnapshot(f.getValue(), m.name(), p.name(), l.name(), a.name());
    }

    private LineHeight raise(LineHeight h) {
        return h == LineHeight.LOW ? LineHeight.NORMAL : LineHeight.HIGH;
    }

    private Pressing lower(Pressing p) {
        return p == Pressing.HIGH ? Pressing.NORMAL : Pressing.LOW;
    }
}
