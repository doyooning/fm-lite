package com.fmlite.match.analysis;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.MatchAccess;
import com.fmlite.match.MatchAccess.MatchContext;
import com.fmlite.match.dto.OpponentAnalysisResponse;
import com.fmlite.match.dto.OpponentAnalysisResponse.KeyPlayer;
import com.fmlite.match.dto.OpponentAnalysisResponse.OpponentTeam;
import com.fmlite.match.dto.TacticDto;
import com.fmlite.match.simulation.OpponentAiService;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerRepository;
import com.fmlite.player.PlayerTrait;
import com.fmlite.player.Position;
import com.fmlite.team.Team;
import com.fmlite.team.TeamRepository;
import com.fmlite.team.dto.TeamDetailResponse;
import com.fmlite.team.dto.TeamDetailResponse.PowerByArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 규칙 기반 상대 팀 분석 (확장 시 이 결과를 LLM 프롬프트 입력으로 재사용) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpponentAnalysisService {

    private final MatchAccess matchAccess;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final OpponentAiService opponentAiService;

    public OpponentAnalysisResponse analyze(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        if (!ctx.match().isUserMatch()) {
            throw BusinessException.conflict("NOT_USER_MATCH", "사용자 팀의 경기가 아닙니다.");
        }
        Long opponentId = ctx.match().opponentOf(ctx.userTeamId());
        Team opponent = teamRepository.findById(opponentId)
                .orElseThrow(() -> BusinessException.notFound("팀"));
        List<Player> players = playerRepository.findByTeamId(opponentId);
        List<Player> myPlayers = playerRepository.findByTeamId(ctx.userTeamId());

        PowerByArea power = TeamDetailResponse.powerByArea(players);

        return new OpponentAnalysisResponse(
                new OpponentTeam(opponent.getId(), opponent.getName(), opponent.getShortName(),
                        opponent.getGrade().name(), opponent.getGrade().getLabel()),
                power,
                TeamDetailResponse.powerByArea(myPlayers),
                strengths(power, players),
                weaknesses(power, players),
                keyPlayers(players),
                TacticDto.from(opponentAiService.initialTactic(opponent))
        );
    }

    private List<String> strengths(PowerByArea power, List<Player> players) {
        List<String> result = new ArrayList<>();
        if (power.attack() >= 74) result.add("공격진의 결정력이 위협적입니다. 뒷공간 관리에 주의하세요.");
        if (power.midfield() >= 74) result.add("미드필드 패스 전개가 안정적이라 중원 싸움이 쉽지 않습니다.");
        if (power.defense() >= 74) result.add("수비 조직력이 견고해 정면 돌파가 어렵습니다.");
        if (power.goalkeeping() >= 76) result.add("골키퍼가 안정적이라 유효슛 대비 실점이 적습니다.");
        if (hasTrait(players, PlayerTrait.PASS_MASTER)) result.add("중원에 패스 마스터가 있어 점유율 싸움에 강합니다.");
        if (players.stream().anyMatch(p -> p.getPosition() == Position.GK && p.hasTrait(PlayerTrait.PK_SAVER))) {
            result.add("PK 선방에 능한 골키퍼가 있어 승부차기는 피하는 편이 좋습니다.");
        }
        if (result.isEmpty()) result.add("특별히 두드러진 강점은 없는 팀입니다.");
        return result;
    }

    private List<String> weaknesses(PowerByArea power, List<Player> players) {
        List<String> result = new ArrayList<>();
        if (power.attack() <= 66) result.add("공격 결정력이 부족해 실점 위험은 낮은 편입니다.");
        if (power.midfield() <= 66) result.add("중원 장악력이 약해 압박에 취약합니다.");
        if (power.defense() <= 66) result.add("수비 조직이 불안해 꾸준한 공격이 통합니다.");
        double dfSpeed = players.stream().filter(p -> p.getPosition() == Position.DF)
                .mapToInt(Player::getSpeed).average().orElse(70);
        if (dfSpeed < 66) result.add("수비진 스피드가 느려 뒷공간 침투에 취약합니다.");
        if (countTrait(players, PlayerTrait.LOW_STAMINA) >= 2) {
            result.add("체력이 약한 선수가 많아 후반 강한 압박이 효과적입니다.");
        }
        if (countTrait(players, PlayerTrait.WEAK_UNDER_PRESSURE) >= 2) {
            result.add("압박에 흔들리는 선수가 많습니다. 높은 압박을 고려하세요.");
        }
        if (result.isEmpty()) result.add("뚜렷한 약점이 없는 균형 잡힌 팀입니다. 전술 상성으로 승부하세요.");
        return result;
    }

    private List<KeyPlayer> keyPlayers(List<Player> players) {
        List<KeyPlayer> result = new ArrayList<>();
        players.stream()
                .filter(p -> p.getPosition() != Position.GK)   // GK는 특성 기반으로만 노출
                .sorted(Comparator.comparingInt(Player::overall).reversed())
                .limit(2)
                .forEach(p -> result.add(new KeyPlayer(p.getId(), p.getName(), p.getPosition().name(),
                        p.overall(), "팀 내 최고 수준의 종합 능력치")));
        players.stream()
                .filter(p -> result.stream().noneMatch(k -> k.id().equals(p.getId())))
                .filter(p -> p.hasTrait(PlayerTrait.BIG_GAME_PLAYER) || p.hasTrait(PlayerTrait.PASS_MASTER)
                        || p.hasTrait(PlayerTrait.RUN_IN_BEHIND))
                .max(Comparator.comparingInt(Player::overall))
                .ifPresent(p -> result.add(new KeyPlayer(p.getId(), p.getName(), p.getPosition().name(),
                        p.overall(), p.traitList().get(0).getLabel() + " 특성 보유")));
        return result;
    }

    private boolean hasTrait(List<Player> players, PlayerTrait trait) {
        return players.stream().anyMatch(p -> p.hasTrait(trait));
    }

    private long countTrait(List<Player> players, PlayerTrait trait) {
        return players.stream().filter(p -> p.hasTrait(trait)).count();
    }
}
