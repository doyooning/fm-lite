package com.fmlite.profile;

import com.fmlite.common.response.ApiResponse;
import com.fmlite.profile.dto.ProfileResponse;
import com.fmlite.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> me(@CurrentUserId UUID userId) {
        return ApiResponse.ok(profileService.getProfile(userId));
    }
}
