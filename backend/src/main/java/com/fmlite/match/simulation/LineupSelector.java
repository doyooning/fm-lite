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
}
