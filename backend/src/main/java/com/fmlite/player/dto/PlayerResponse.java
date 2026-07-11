package com.fmlite.player.dto;

import com.fmlite.player.Player;

import java.util.List;

public record PlayerResponse(
        Long id,
        String name,
        String position,
        int backNumber,
        int overall,
        Stats stats,
        List<Trait> traits
) {
    public record Stats(int attack, int defense, int passing, int speed,
                        int stamina, int mentality, int finishing, int goalkeeping) {}

    public record Trait(String code, String name, String description) {}

    public static PlayerResponse from(Player p) {
        return new PlayerResponse(
                p.getId(), p.getName(), p.getPosition().name(), p.getBackNumber(), p.overall(),
                new Stats(p.getAttack(), p.getDefense(), p.getPassing(), p.getSpeed(),
                        p.getStamina(), p.getMentality(), p.getFinishing(), p.getGoalkeeping()),
                p.traitList().stream()
                        .map(t -> new Trait(t.name(), t.getLabel(), t.getDescription()))
                        .toList()
        );
    }
}
