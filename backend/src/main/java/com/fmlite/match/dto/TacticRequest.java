package com.fmlite.match.dto;

import com.fmlite.match.tactic.AttackStyle;
import com.fmlite.match.tactic.Formation;
import com.fmlite.match.tactic.LineHeight;
import com.fmlite.match.tactic.Mentality;
import com.fmlite.match.tactic.Pressing;
import jakarta.validation.constraints.NotNull;

public record TacticRequest(
        @NotNull Formation formation,
        @NotNull Mentality mentality,
        @NotNull Pressing pressing,
        @NotNull LineHeight lineHeight,
        @NotNull AttackStyle attackStyle
) {}
