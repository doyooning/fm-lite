package com.fmlite.competition;

import com.fmlite.common.response.ApiResponse;
import com.fmlite.competition.dto.BracketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping("/{competitionId}")
    public ApiResponse<BracketResponse> bracket(@PathVariable Long competitionId) {
        return ApiResponse.ok(competitionService.bracket(competitionId));
    }
}
