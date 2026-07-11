package com.fmlite.competition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Optional<Competition> findBySaveGameId(Long saveGameId);
}
