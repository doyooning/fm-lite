package com.fmlite.match;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.MatchAccess.MatchContext;
import com.fmlite.match.dto.MatchInfoResponse;
import com.fmlite.match.dto.MatchProgressResponse;
import com.fmlite.match.dto.MatchResultResponse;
import com.fmlite.match.dto.TeamBrief;
import com.fmlite.match.result.MatchResult;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.match.tactic.TacticRepository;
import com.fmlite.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchAccess matchAccess;
    private final TeamRepository teamRepository;
    private final TacticRepository tacticRepository;
    private final MatchResultRepository matchResultRepository;

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

    private TeamBrief brief(Long teamId) {
        return teamRepository.findById(teamId).map(TeamBrief::from)
                .orElse(new TeamBrief(teamId, "팀 " + teamId, "?"));
    }
}
