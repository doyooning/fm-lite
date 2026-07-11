package com.fmlite.match.dto;

import java.util.List;

public record MatchProgressResponse(
        String matchStatus,
        Score score,
        int minute,
        List<MatchEventResponse> events
) {
    public record Score(int home, int away) {}
}
