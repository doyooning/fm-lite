package com.fmlite.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(UUID userId);

    boolean existsByUserIdAndCode(UUID userId, Achievement code);
}
