package com.fmlite.match.simulation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fmlite.match.tactic.AttackStyle;
import com.fmlite.match.tactic.Formation;
import com.fmlite.match.tactic.LineHeight;
import com.fmlite.match.tactic.Mentality;
import com.fmlite.match.tactic.Pressing;
import com.fmlite.match.tactic.Tactic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 경기 중 전술 상태 (AI는 경기 중 변경 가능) — simulation_state jsonb에 저장 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TacticSnapshot {

    private String formation;    // "4-3-3" 등 표시값
    private String mentality;
    private String pressing;
    private String lineHeight;
    private String attackStyle;

    public static TacticSnapshot from(Tactic tactic) {
        return new TacticSnapshot(
                tactic.getFormation().getValue(),
                tactic.getMentality().name(),
                tactic.getPressing().name(),
                tactic.getLineHeight().name(),
                tactic.getAttackStyle().name()
        );
    }

    @JsonIgnore public Formation formationEnum() { return Formation.fromValue(formation); }
    @JsonIgnore public Mentality mentalityEnum() { return Mentality.valueOf(mentality); }
    @JsonIgnore public Pressing pressingEnum() { return Pressing.valueOf(pressing); }
    @JsonIgnore public LineHeight lineHeightEnum() { return LineHeight.valueOf(lineHeight); }
    @JsonIgnore public AttackStyle attackStyleEnum() { return AttackStyle.valueOf(attackStyle); }
}
