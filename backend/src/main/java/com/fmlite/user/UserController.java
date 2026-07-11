package com.fmlite.user;

import com.fmlite.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    public record CreateUserRequest(String nickname) {}

    public record UserResponse(UUID id, String nickname) {}

    @PostMapping
    public ApiResponse<UserResponse> create(@RequestBody(required = false) CreateUserRequest request) {
        String nickname = request == null ? null : request.nickname();
        User user = userRepository.save(new User(nickname));
        return ApiResponse.ok(new UserResponse(user.getId(), user.getNickname()));
    }
}
