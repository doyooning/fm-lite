package com.fmlite.savegame;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SaveGameRepository extends JpaRepository<SaveGame, Long> {

    List<SaveGame> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);
}
