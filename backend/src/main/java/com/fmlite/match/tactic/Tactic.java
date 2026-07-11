package com.fmlite.match.tactic;

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

import java.time.Instant;

@Entity
@Table(name = "tactics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tactic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private Formation formation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mentality mentality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Pressing pressing;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_height", nullable = false)
    private LineHeight lineHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "attack_style", nullable = false)
    private AttackStyle attackStyle;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Tactic(Long matchId, Long teamId, Formation formation, Mentality mentality,
                  Pressing pressing, LineHeight lineHeight, AttackStyle attackStyle) {
        this.matchId = matchId;
        this.teamId = teamId;
        update(formation, mentality, pressing, lineHeight, attackStyle);
    }

    public void update(Formation formation, Mentality mentality, Pressing pressing,
                       LineHeight lineHeight, AttackStyle attackStyle) {
        this.formation = formation;
        this.mentality = mentality;
        this.pressing = pressing;
        this.lineHeight = lineHeight;
        this.attackStyle = attackStyle;
        this.updatedAt = Instant.now();
    }
}
