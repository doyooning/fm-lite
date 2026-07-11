package com.fmlite.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamIdOrderByPositionAscBackNumberAsc(Long teamId);

    List<Player> findByTeamId(Long teamId);
}
