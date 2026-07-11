package com.fmlite.match.event;

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

import java.util.List;

@Entity
@Table(name = "match_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(nullable = false)
    private int seq;

    @Column(nullable = false)
    private int minute;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private MatchEventType eventType;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(nullable = false)
    private String description;

    @Column(name = "requires_choice", nullable = false)
    private boolean requiresChoice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "choice_options")
    private List<ChoiceOption> choiceOptions;

    @Column(name = "selected_choice_id")
    private String selectedChoiceId;

    public MatchEvent(Long matchId, int seq, int minute, MatchEventType eventType,
                      Long teamId, Long playerId, String description,
                      boolean requiresChoice, List<ChoiceOption> choiceOptions) {
        this.matchId = matchId;
        this.seq = seq;
        this.minute = minute;
        this.eventType = eventType;
        this.teamId = teamId;
        this.playerId = playerId;
        this.description = description;
        this.requiresChoice = requiresChoice;
        this.choiceOptions = choiceOptions;
    }

    public void selectChoice(String choiceId) {
        this.selectedChoiceId = choiceId;
    }
}
