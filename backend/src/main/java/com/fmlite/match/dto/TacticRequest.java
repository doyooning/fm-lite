package com.fmlite.match.dto;

import com.fmlite.match.tactic.AttackStyle;
import com.fmlite.match.tactic.Formation;
import com.fmlite.match.tactic.LineHeight;
import com.fmlite.match.tactic.Mentality;
import com.fmlite.match.tactic.Pressing;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TacticRequest(
        @NotNull Formation formation,
        @NotNull Mentality mentality,
        @NotNull Pressing pressing,
        @NotNull LineHeight lineHeight,
        @NotNull AttackStyle attackStyle,
        // 선발 11명 (player_id). 미지정(null)이면 서버가 포메이션 기준 베스트 XI 로 자동 선발
        List<Long> lineup
) {}
