package com.fmlite.match.tactic;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.MatchAccess;
import com.fmlite.match.MatchAccess.MatchContext;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.dto.TacticDto;
import com.fmlite.match.dto.TacticRequest;
import com.fmlite.match.dto.TacticResponse;
import com.fmlite.match.simulation.LineupSelector;
import com.fmlite.player.Player;
import com.fmlite.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TacticService {

    private static final Formation DEFAULT_FORMATION = Formation.F_4_3_3;

    private final MatchAccess matchAccess;
    private final TacticRepository tacticRepository;
    private final PlayerRepository playerRepository;
    private final LineupSelector lineupSelector;

    @Transactional(readOnly = true)
    public TacticResponse getMyTactic(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        List<Player> squad = playerRepository.findByTeamId(ctx.userTeamId());

        return tacticRepository.findByMatchIdAndTeamId(matchId, ctx.userTeamId())
                .map(t -> {
                    // 레거시(라인업 미저장) 전술이면 베스트 XI 를 채워 반환
                    List<Long> lineup = t.getLineup() != null
                            ? t.getLineup() : lineupSelector.select(squad, t.getFormation());
                    return new TacticResponse(true, new TacticDto(
                            t.getFormation().getValue(), t.getMentality().name(), t.getPressing().name(),
                            t.getLineHeight().name(), t.getAttackStyle().name(), lineup));
                })
                .orElseGet(() -> new TacticResponse(false, new TacticDto(
                        DEFAULT_FORMATION.getValue(), "BALANCED", "NORMAL", "NORMAL", "CENTER",
                        lineupSelector.select(squad, DEFAULT_FORMATION))));
    }

    @Transactional
    public TacticResponse saveMyTactic(Long matchId, UUID userId, TacticRequest request) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        if (!ctx.match().isUserMatch() || !ctx.match().involves(ctx.userTeamId())) {
            throw BusinessException.conflict("NOT_USER_MATCH", "사용자 팀의 경기가 아닙니다.");
        }
        if (ctx.match().getStatus() != MatchStatus.SCHEDULED) {
            throw BusinessException.conflict("MATCH_ALREADY_STARTED", "이미 시작된 경기의 전술은 변경할 수 없습니다.");
        }

        List<Long> lineup = resolveLineup(ctx.userTeamId(), request);

        Tactic tactic = tacticRepository.findByMatchIdAndTeamId(matchId, ctx.userTeamId())
                .map(t -> {
                    t.update(request.formation(), request.mentality(), request.pressing(),
                            request.lineHeight(), request.attackStyle(), lineup);
                    return t;
                })
                .orElseGet(() -> tacticRepository.save(new Tactic(matchId, ctx.userTeamId(),
                        request.formation(), request.mentality(), request.pressing(),
                        request.lineHeight(), request.attackStyle(), lineup)));

        return new TacticResponse(true, TacticDto.from(tactic));
    }

    /** 요청 라인업 검증(있으면) 또는 베스트 XI 자동 선발(없으면) */
    public List<Long> resolveLineup(Long teamId, TacticRequest request) {
        List<Player> squad = playerRepository.findByTeamId(teamId);
        if (request.lineup() != null && !request.lineup().isEmpty()) {
            if (!lineupSelector.isValidLineup(request.lineup(), squad, request.formation())) {
                throw BusinessException.badRequest("INVALID_LINEUP",
                        "선발 라인업이 포메이션 규칙(포지션별 인원)에 맞지 않습니다.");
            }
            return request.lineup();
        }
        return lineupSelector.select(squad, request.formation());
    }
}
