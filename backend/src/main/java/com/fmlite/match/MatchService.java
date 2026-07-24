package com.fmlite.match;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.MatchAccess.MatchContext;
import com.fmlite.match.dto.MatchInfoResponse;
import com.fmlite.match.dto.MatchProgressResponse;
import com.fmlite.match.dto.MatchResultResponse;
import com.fmlite.match.dto.OpponentTacticResponse;
import com.fmlite.match.dto.TeamBrief;
import com.fmlite.match.event.MatchEventRepository;
import com.fmlite.match.event.MatchEventType;
import com.fmlite.match.result.MatchResult;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.match.simulation.OpponentAiService;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.match.simulation.model.TacticSnapshot;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.TacticRepository;
import com.fmlite.team.Team;
import com.fmlite.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchAccess matchAccess;
    private final TeamRepository teamRepository;
    private final TacticRepository tacticRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchEventRepository matchEventRepository;
    private final OpponentAiService opponentAiService;

    public MatchInfoResponse getMatchInfo(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        Match match = ctx.match();
        boolean tacticSubmitted = tacticRepository
                .findByMatchIdAndTeamId(matchId, ctx.userTeamId()).isPresent();
        MatchProgressResponse.Score score = match.getSimulationState() == null ? null
                : new MatchProgressResponse.Score(match.getSimulationState().getHomeScore(),
                        match.getSimulationState().getAwayScore());
        return new MatchInfoResponse(
                match.getId(), match.getRound().name(), match.getRound().getLabel(),
                match.getStatus().name(),
                brief(match.getHomeTeamId()), brief(match.getAwayTeamId()),
                match.isUserMatch(), match.getHomeTeamId().equals(ctx.userTeamId()),
                tacticSubmitted, score);
    }

    public MatchResultResponse getResult(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        Match match = ctx.match();
        if (match.getStatus() != MatchStatus.FINISHED) {
            throw BusinessException.conflict("MATCH_NOT_FINISHED", "아직 종료되지 않은 경기입니다.");
        }
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> BusinessException.notFound("경기 결과"));

        Boolean userWon = match.isUserMatch()
                ? result.getWinnerTeamId().equals(ctx.userTeamId()) : null;

        return new MatchResultResponse(
                match.getId(), match.getRound().name(), match.getRound().getLabel(),
                brief(match.getHomeTeamId()), brief(match.getAwayTeamId()),
                result.getHomeScore(), result.getAwayScore(),
                result.getPenaltyHomeScore(), result.getPenaltyAwayScore(),
                result.getWinnerTeamId(), userWon,
                ctx.saveGame().getStatus().name(), result.getStats());
    }

    /**
     * 경기 중 상대 팀의 현재 전술. 진행 중이면 시뮬레이션 상태(AI 변경 반영)를,
     * 경기 전이면 저장된/예상 전술을 돌려준다.
     */
    public OpponentTacticResponse getOpponentTactic(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        Match match = ctx.match();
        Long opponentId = match.opponentOf(ctx.userTeamId());
        Team opponent = teamRepository.findById(opponentId)
                .orElseThrow(() -> BusinessException.notFound("팀"));

        SimulationState state = match.getSimulationState();
        boolean live = false;
        TacticSnapshot snapshot;

        if (state != null) {
            TeamSimState side = state.getHome().getTeamId() == opponentId ? state.getHome() : state.getAway();
            snapshot = side.getTactic();
            live = true;
        } else {
            snapshot = tacticRepository.findByMatchIdAndTeamId(matchId, opponentId)
                    .map(TacticSnapshot::from)
                    .orElseGet(() -> opponentAiService.initialTactic(opponent));
        }

        // 상대의 전술 변화 로그(최근 5건)
        List<String> changes = matchEventRepository.findByMatchIdOrderBySeqAsc(matchId).stream()
                .filter(e -> e.getEventType() == MatchEventType.TACTIC_CHANGE
                        && opponentId.equals(e.getTeamId()))
                .sorted(Comparator.comparingInt(e -> -e.getSeq()))
                .limit(5)
                .map(e -> e.getMinute() + "' " + e.getDescription())
                .toList();

        return new OpponentTacticResponse(
                opponent.getId(), opponent.getName(),
                snapshot.getFormation(),
                snapshot.getMentality(), label(snapshot.getMentality()),
                snapshot.getPressing(), label(snapshot.getPressing()),
                snapshot.getLineHeight(), label(snapshot.getLineHeight()),
                snapshot.getAttackStyle(), label(snapshot.getAttackStyle()),
                live, changes);
    }

    private String label(String code) {
        return switch (code) {
            case "ATTACKING" -> "공격적";
            case "BALANCED" -> "균형";
            case "DEFENSIVE" -> "수비적";
            case "LOW" -> "낮음";
            case "NORMAL" -> "보통";
            case "HIGH" -> "높음";
            case "CENTER" -> "중앙";
            case "WIDE" -> "측면";
            case "COUNTER" -> "역습";
            case "POSSESSION" -> "점유";
            default -> code;
        };
    }

    private TeamBrief brief(Long teamId) {
        return teamRepository.findById(teamId).map(TeamBrief::from)
                .orElse(new TeamBrief(teamId, "팀 " + teamId, "?"));
    }
}
