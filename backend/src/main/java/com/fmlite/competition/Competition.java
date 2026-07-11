package com.fmlite.competition;

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

@Entity
@Table(name = "competitions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "save_game_id", nullable = false)
    private Long saveGameId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_round", nullable = false)
    private Round currentRound;

    @Column(name = "winner_team_id")
    private Long winnerTeamId;

    public Competition(Long saveGameId) {
        this.saveGameId = saveGameId;
        this.name = "FM 챔피언스 컵";
        this.type = "SINGLE_ELIM_8";
        this.currentRound = Round.QF;
    }

    public void advanceTo(Round round) {
        this.currentRound = round;
    }

    public void finish(Long winnerTeamId) {
        this.currentRound = Round.FINISHED;
        this.winnerTeamId = winnerTeamId;
    }
}
