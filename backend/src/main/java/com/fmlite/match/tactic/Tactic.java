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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

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

    /** 사용자 지정 선발 11명 (player_id 순서). null 이면 포메이션 기준 베스트 XI 자동 선발 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Long> lineup;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Tactic(Long matchId, Long teamId, Formation formation, Mentality mentality,
                  Pressing pressing, LineHeight lineHeight, AttackStyle attackStyle, List<Long> lineup) {
        this.matchId = matchId;
        this.teamId = teamId;
        update(formation, mentality, pressing, lineHeight, attackStyle, lineup);
    }

    public void update(Formation formation, Mentality mentality, Pressing pressing,
                       LineHeight lineHeight, AttackStyle attackStyle, List<Long> lineup) {
        this.formation = formation;
        this.mentality = mentality;
        this.pressing = pressing;
        this.lineHeight = lineHeight;
        this.attackStyle = attackStyle;
        this.lineup = lineup;
        this.updatedAt = Instant.now();
    }
}
