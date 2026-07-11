package com.fmlite.match.result;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "home_score", nullable = false)
    private int homeScore;

    @Column(name = "away_score", nullable = false)
    private int awayScore;

    @Column(name = "penalty_home_score")
    private Integer penaltyHomeScore;

    @Column(name = "penalty_away_score")
    private Integer penaltyAwayScore;

    @Column(name = "winner_team_id", nullable = false)
    private Long winnerTeamId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private MatchStats stats;

    public MatchResult(Long matchId, int homeScore, int awayScore,
                       Integer penaltyHomeScore, Integer penaltyAwayScore,
                       Long winnerTeamId, MatchStats stats) {
        this.matchId = matchId;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.penaltyHomeScore = penaltyHomeScore;
        this.penaltyAwayScore = penaltyAwayScore;
        this.winnerTeamId = winnerTeamId;
        this.stats = stats;
    }
}
