package com.fmlite.match;

import com.fmlite.competition.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByCompetitionIdOrderByRoundAscMatchNoAsc(Long competitionId);

    List<Match> findByCompetitionIdAndRoundOrderByMatchNoAsc(Long competitionId, Round round);
}
