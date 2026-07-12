package com.fmlite.match.simulation;

import com.fmlite.match.tactic.Formation;
import com.fmlite.player.Player;
import com.fmlite.player.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 포메이션 슬롯에 맞춰 포지션별 종합치 상위 선수로 베스트 XI 자동 선발 */
@Component
public class LineupSelector {

    public List<Long> select(List<Player> squad, Formation formation) {
        List<Long> lineup = new ArrayList<>();
        formation.getSlots().forEach((position, count) ->
                squad.stream()
                        .filter(p -> p.getPosition() == position)
                        .sorted(Comparator.comparingInt(Player::overall).reversed())
                        .limit(count)
                        .forEach(p -> lineup.add(p.getId()))
        );
        return lineup;
    }

    public List<Player> lineupPlayers(List<Player> squad, List<Long> lineup) {
        return squad.stream().filter(p -> lineup.contains(p.getId())).toList();
    }

    public List<Player> lineupByPosition(List<Player> squad, List<Long> lineup, Position position) {
        return squad.stream()
                .filter(p -> lineup.contains(p.getId()) && p.getPosition() == position)
                .toList();
    }

    /**
     * 사용자 지정 라인업이 포메이션 규칙에 맞는지 검증한다.
     * - 정확히 11명 / 중복 없음 / 모두 해당 팀 소속 / 포지션별 인원이 포메이션 슬롯과 일치
     * @return 유효하면 true
     */
    public boolean isValidLineup(List<Long> lineup, List<Player> squad, Formation formation) {
        if (lineup == null || lineup.size() != 11) return false;
        if (lineup.stream().distinct().count() != 11) return false;

        java.util.Map<Long, Player> byId = new java.util.HashMap<>();
        squad.forEach(p -> byId.put(p.getId(), p));
        if (!byId.keySet().containsAll(lineup)) return false;   // 팀 소속이 아닌 선수 포함

        java.util.Map<Position, Integer> counts = new java.util.EnumMap<>(Position.class);
        for (Long id : lineup) {
            Position pos = byId.get(id).getPosition();
            counts.merge(pos, 1, Integer::sum);
        }
        for (var slot : formation.getSlots().entrySet()) {
            if (counts.getOrDefault(slot.getKey(), 0) != slot.getValue()) return false;
        }
        return true;
    }
}
