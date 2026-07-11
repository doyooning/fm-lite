package com.fmlite.match.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** match_results.stats(jsonb)에 저장되는 경기 요약 통계 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchStats {
    private int possessionHome;      // %
    private int possessionAway;
    private int shotsHome;
    private int shotsAway;
    private int shotsOnTargetHome;
    private int shotsOnTargetAway;
    private Long bestPlayerId;
    private String bestPlayerName;
}
