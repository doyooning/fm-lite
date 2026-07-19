package com.fmlite.match.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    List<MatchEvent> findByMatchIdOrderBySeqAsc(Long matchId);

    List<MatchEvent> findByMatchIdAndSeqGreaterThanOrderBySeqAsc(Long matchId, int afterSeq);

    Optional<MatchEvent> findFirstByMatchIdAndRequiresChoiceTrueAndSelectedChoiceIdIsNullOrderBySeqDesc(Long matchId);

    Optional<MatchEvent> findFirstByMatchIdOrderBySeqDesc(Long matchId);

    void deleteByMatchIdIn(List<Long> matchIds);
}
