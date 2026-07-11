package com.fmlite.match;

import com.fmlite.common.response.ApiResponse;
import com.fmlite.match.analysis.OpponentAnalysisService;
import com.fmlite.match.dto.ChoiceRequest;
import com.fmlite.match.dto.MatchInfoResponse;
import com.fmlite.match.dto.MatchProgressResponse;
import com.fmlite.match.dto.MatchResultResponse;
import com.fmlite.match.dto.OpponentAnalysisResponse;
import com.fmlite.match.dto.TacticRequest;
import com.fmlite.match.dto.TacticResponse;
import com.fmlite.match.simulation.MatchSimulationService;
import com.fmlite.match.tactic.TacticService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches/{matchId}")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final TacticService tacticService;
    private final OpponentAnalysisService opponentAnalysisService;
    private final MatchSimulationService matchSimulationService;

    @GetMapping
    public ApiResponse<MatchInfoResponse> getMatch(@PathVariable Long matchId,
                                                   @RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(matchService.getMatchInfo(matchId, userId));
    }

    @GetMapping("/opponent-analysis")
    public ApiResponse<OpponentAnalysisResponse> opponentAnalysis(@PathVariable Long matchId,
                                                                  @RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(opponentAnalysisService.analyze(matchId, userId));
    }

    @GetMapping("/tactics/me")
    public ApiResponse<TacticResponse> getMyTactic(@PathVariable Long matchId,
                                                   @RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(tacticService.getMyTactic(matchId, userId));
    }

    @PutMapping("/tactics/me")
    public ApiResponse<TacticResponse> saveMyTactic(@PathVariable Long matchId,
                                                    @RequestHeader("X-User-Id") UUID userId,
                                                    @Valid @RequestBody TacticRequest request) {
        return ApiResponse.ok(tacticService.saveMyTactic(matchId, userId, request));
    }

    @PostMapping("/start")
    public ApiResponse<MatchProgressResponse> start(@PathVariable Long matchId,
                                                    @RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(matchSimulationService.start(matchId, userId));
    }

    @PostMapping("/choices")
    public ApiResponse<MatchProgressResponse> choose(@PathVariable Long matchId,
                                                     @RequestHeader("X-User-Id") UUID userId,
                                                     @Valid @RequestBody ChoiceRequest request) {
        return ApiResponse.ok(matchSimulationService.submitChoice(matchId, userId, request));
    }

    @GetMapping("/events")
    public ApiResponse<MatchProgressResponse> events(@PathVariable Long matchId,
                                                     @RequestParam(required = false) Integer afterSeq) {
        return ApiResponse.ok(matchSimulationService.getEvents(matchId, afterSeq));
    }

    @GetMapping("/result")
    public ApiResponse<MatchResultResponse> result(@PathVariable Long matchId,
                                                   @RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(matchService.getResult(matchId, userId));
    }
}
