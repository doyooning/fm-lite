package com.fmlite.match.result;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    Optional<MatchResult> findByMatchId(Long matchId);

    List<MatchResult> findByMatchIdIn(List<Long> matchIds);

    void deleteByMatchIdIn(List<Long> matchIds);
}
