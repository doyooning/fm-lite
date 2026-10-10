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
        // 슬롯 순서(GK → DF → MF → FW)대로 채운다 — 라인업 인덱스가 곧 슬롯 위치이므로 순서가 중요하다
        List<Long> lineup = new ArrayList<>();
        for (Position position : new Position[]{Position.GK, Position.DF, Position.MF, Position.FW}) {
            squad.stream()
                    .filter(p -> p.getPosition() == position)
                    .sorted(Comparator.comparingInt(Player::overall).reversed())
                    .limit(formation.getSlots().getOrDefault(position, 0))
                    .forEach(p -> lineup.add(p.getId()));
        }
        return lineup;
    }

    /** 라인업 슬롯: 선수 + 해당 슬롯에서 맡는 역할(본래 포지션과 다를 수 있음) */
    public record Slot(Player player, Position role) {}

    /** 라인업 순서(=포메이션 슬롯 순서)대로 선수 반환 */
    public List<Player> lineupPlayers(List<Player> squad, List<Long> lineup) {
        java.util.Map<Long, Player> byId = new java.util.HashMap<>();
        squad.forEach(p -> byId.put(p.getId(), p));
        return lineup.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    /** 라인업 + 포메이션 → 슬롯(선수, 역할) 목록. 슬롯 i 의 역할은 포메이션의 i번째 슬롯 역할 */
    public List<Slot> slots(List<Player> squad, List<Long> lineup, Formation formation) {
        List<Player> players = lineupPlayers(squad, lineup);
        List<Position> roles = formation.slotRoles();
        List<Slot> result = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            result.add(new Slot(players.get(i), roles.get(Math.min(i, roles.size() - 1))));
        }
        return result;
    }

    /**
     * 사용자 지정 라인업 검증: 정확히 11명 / 중복 없음 / 모두 해당 팀 소속.
     * 포지션 제약은 없다 - 어느 선수든 어느 슬롯에든 배치할 수 있다.
     * (formation 인자는 슬롯 수 확인용)
     */
    public boolean isValidLineup(List<Long> lineup, List<Player> squad, Formation formation) {
        if (lineup == null || lineup.size() != formation.slotRoles().size()) return false;
        if (lineup.stream().distinct().count() != lineup.size()) return false;
        java.util.Set<Long> ids = new java.util.HashSet<>();
        squad.forEach(p -> ids.add(p.getId()));
        return ids.containsAll(lineup);
    }
}
