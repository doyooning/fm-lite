package com.fmlite.match;

import com.fmlite.competition.Round;
import com.fmlite.match.simulation.model.SimulationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Round round;

    @Column(name = "match_no", nullable = false)
    private int matchNo;

    @Column(name = "home_team_id", nullable = false)
    private Long homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private Long awayTeamId;

    @Column(name = "is_user_match", nullable = false)
    private boolean userMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "simulation_state")
    private SimulationState simulationState;

    public Match(Long competitionId, Round round, int matchNo,
                 Long homeTeamId, Long awayTeamId, boolean userMatch) {
        this.competitionId = competitionId;
        this.round = round;
        this.matchNo = matchNo;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.userMatch = userMatch;
        this.status = MatchStatus.SCHEDULED;
    }

    public void updateState(SimulationState state, MatchStatus status) {
        this.simulationState = state;
        this.status = status;
    }

    public boolean involves(Long teamId) {
        return homeTeamId.equals(teamId) || awayTeamId.equals(teamId);
    }

    public Long opponentOf(Long teamId) {
        return homeTeamId.equals(teamId) ? awayTeamId : homeTeamId;
    }
}
