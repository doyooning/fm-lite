package com.fmlite.match.tactic;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.MatchAccess;
import com.fmlite.match.MatchAccess.MatchContext;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.dto.TacticDto;
import com.fmlite.match.dto.TacticRequest;
import com.fmlite.match.dto.TacticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TacticService {

    private static final TacticDto DEFAULT_TACTIC =
            new TacticDto("4-3-3", "BALANCED", "NORMAL", "NORMAL", "CENTER");

    private final MatchAccess matchAccess;
    private final TacticRepository tacticRepository;

    @Transactional(readOnly = true)
    public TacticResponse getMyTactic(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        return tacticRepository.findByMatchIdAndTeamId(matchId, ctx.userTeamId())
                .map(t -> new TacticResponse(true, TacticDto.from(t)))
                .orElseGet(() -> new TacticResponse(false, DEFAULT_TACTIC));
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

        Tactic tactic = tacticRepository.findByMatchIdAndTeamId(matchId, ctx.userTeamId())
                .map(t -> {
                    t.update(request.formation(), request.mentality(), request.pressing(),
                            request.lineHeight(), request.attackStyle());
                    return t;
                })
                .orElseGet(() -> tacticRepository.save(new Tactic(matchId, ctx.userTeamId(),
                        request.formation(), request.mentality(), request.pressing(),
                        request.lineHeight(), request.attackStyle())));

        return new TacticResponse(true, TacticDto.from(tactic));
    }
}
