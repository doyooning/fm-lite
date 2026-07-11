package com.fmlite.savegame;

import com.fmlite.common.response.ApiResponse;
import com.fmlite.savegame.dto.NextMatchResponse;
import com.fmlite.savegame.dto.SaveGameResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SaveGameController {

    private final SaveGameService saveGameService;

    public record CreateSaveGameRequest(@NotNull Long teamId) {}

    @PostMapping("/save-games")
    public ApiResponse<SaveGameResponse> create(@RequestHeader("X-User-Id") UUID userId,
                                                @Valid @RequestBody CreateSaveGameRequest request) {
        return ApiResponse.ok(saveGameService.create(userId, request.teamId()));
    }

    @GetMapping("/save-games/{saveGameId}")
    public ApiResponse<SaveGameResponse> get(@PathVariable Long saveGameId) {
        return ApiResponse.ok(saveGameService.get(saveGameId));
    }

    @GetMapping("/save-games/{saveGameId}/next-match")
    public ApiResponse<NextMatchResponse> nextMatch(@PathVariable Long saveGameId) {
        return ApiResponse.ok(saveGameService.nextMatch(saveGameId));
    }

    @GetMapping("/users/{userId}/save-games")
    public ApiResponse<List<SaveGameResponse>> listByUser(@PathVariable UUID userId) {
        return ApiResponse.ok(saveGameService.listByUser(userId));
    }
}
