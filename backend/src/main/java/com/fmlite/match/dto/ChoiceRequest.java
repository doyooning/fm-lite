package com.fmlite.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChoiceRequest(
        @NotNull Long eventId,
        @NotBlank String choiceId
) {}
