package com.fmlite.match.tactic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TacticRepository extends JpaRepository<Tactic, Long> {

    Optional<Tactic> findByMatchIdAndTeamId(Long matchId, Long teamId);

    void deleteByMatchIdIn(List<Long> matchIds);
}
