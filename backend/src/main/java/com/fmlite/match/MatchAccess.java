package com.fmlite.match;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.competition.Competition;
import com.fmlite.competition.CompetitionRepository;
import com.fmlite.savegame.SaveGame;
import com.fmlite.savegame.SaveGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 경기 → 대회 → 저장 게임 컨텍스트 로딩 + 소유자 검증 */
@Component
@RequiredArgsConstructor
public class MatchAccess {

    private final MatchRepository matchRepository;
    private final CompetitionRepository competitionRepository;
    private final SaveGameRepository saveGameRepository;

    public record MatchContext(Match match, Competition competition, SaveGame saveGame, Long userTeamId) {}

    public MatchContext context(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessException.notFound("경기"));
        Competition competition = competitionRepository.findById(match.getCompetitionId())
                .orElseThrow(() -> BusinessException.notFound("대회"));
        SaveGame saveGame = saveGameRepository.findById(competition.getSaveGameId())
                .orElseThrow(() -> BusinessException.notFound("저장 게임"));
        return new MatchContext(match, competition, saveGame, saveGame.getTeamId());
    }

    public MatchContext userContext(Long matchId, UUID userId) {
        MatchContext ctx = context(matchId);
        if (!ctx.saveGame().getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 저장 게임에 대한 권한이 없습니다.");
        }
        return ctx;
    }
}
