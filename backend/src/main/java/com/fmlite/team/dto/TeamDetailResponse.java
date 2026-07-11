package com.fmlite.team.dto;

import com.fmlite.player.Player;
import com.fmlite.player.Position;
import com.fmlite.team.Team;

import java.util.Comparator;
import java.util.List;

public record TeamDetailResponse(
        Long id,
        String name,
        String shortName,
        String grade,
        String gradeLabel,
        String description,
        int avgRating,
        PowerByArea powerByArea,
        List<KeyPlayer> keyPlayers
) {
    public record PowerByArea(int attack, int midfield, int defense, int goalkeeping) {}

    public record KeyPlayer(Long id, String name, String position, int overall) {}

    public static TeamDetailResponse of(Team team, List<Player> players) {
        int avg = (int) Math.round(players.stream().mapToInt(Player::overall).average().orElse(0));
        List<KeyPlayer> key = players.stream()
                .sorted(Comparator.comparingInt(Player::overall).reversed())
                .limit(3)
                .map(p -> new KeyPlayer(p.getId(), p.getName(), p.getPosition().name(), p.overall()))
                .toList();
        return new TeamDetailResponse(team.getId(), team.getName(), team.getShortName(),
                team.getGrade().name(), team.getGrade().getLabel(), team.getDescription(),
                avg, powerByArea(players), key);
    }

    public static PowerByArea powerByArea(List<Player> players) {
        return new PowerByArea(
                avg(players, Position.FW, p -> p.getAttack() * 0.4 + p.getFinishing() * 0.4 + p.getSpeed() * 0.2),
                avg(players, Position.MF, p -> p.getPassing() * 0.5 + p.getStamina() * 0.25 + p.getMentality() * 0.25),
                avg(players, Position.DF, p -> p.getDefense() * 0.6 + p.getSpeed() * 0.2 + p.getMentality() * 0.2),
                players.stream().filter(p -> p.getPosition() == Position.GK)
                        .mapToInt(Player::getGoalkeeping).max().orElse(0)
        );
    }

    private static int avg(List<Player> players, Position pos, java.util.function.ToDoubleFunction<Player> f) {
        return (int) Math.round(players.stream().filter(p -> p.getPosition() == pos)
                .mapToDouble(f).average().orElse(0));
    }
}
