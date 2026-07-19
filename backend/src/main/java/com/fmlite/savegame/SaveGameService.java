package com.fmlite.savegame;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.competition.Competition;
import com.fmlite.competition.CompetitionRepository;
import com.fmlite.match.Match;
import com.fmlite.match.MatchRepository;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.dto.TeamBrief;
import com.fmlite.match.event.MatchEventRepository;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.match.tactic.TacticRepository;
import com.fmlite.savegame.dto.NextMatchResponse;
import com.fmlite.savegame.dto.SaveGameResponse;
import com.fmlite.competition.Round;
import com.fmlite.team.Team;
import com.fmlite.team.TeamGrade;
import com.fmlite.team.TeamRepository;
import com.fmlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaveGameService {

    /** 계정당 최대 저장 게임 수 */
    public static final int MAX_GAMES_PER_USER = 3;

    private final SaveGameRepository saveGameRepository;
    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TacticRepository tacticRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchResultRepository matchResultRepository;
    private final UserRepository userRepository;

    /** 새 게임 시작: SaveGame + Competition + 8강 대진 생성 (강팀 2팀은 반대 사이드 시드 배정) */
    @Transactional
    public SaveGameResponse create(UUID userId, Long teamId, String managerName) {
        // 토큰의 계정이 DB 에 없으면(예: 계정 삭제/초기화) FK 오류 대신 재로그인을 유도한다.
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND",
                    "계정 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
        }
        if (saveGameRepository.countByUserId(userId) >= MAX_GAMES_PER_USER) {
            throw BusinessException.conflict("GAME_LIMIT_REACHED",
                    "게임은 최대 " + MAX_GAMES_PER_USER + "개까지 만들 수 있습니다. 기존 게임을 삭제해 주세요.");
        }
        Team userTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("팀"));

        SaveGame saveGame = saveGameRepository.save(new SaveGame(userId, teamId, managerName));
        Competition competition = competitionRepository.save(new Competition(saveGame.getId()));

        List<Long> slots = drawSlots();
        for (int i = 0; i < 4; i++) {
            Long home = slots.get(i * 2);
            Long away = slots.get(i * 2 + 1);
            boolean userMatch = home.equals(teamId) || away.equals(teamId);
            matchRepository.save(new Match(competition.getId(), Round.QF, i + 1, home, away, userMatch));
        }
        return SaveGameResponse.of(saveGame, userTeam, competition);
    }

    /** 8팀 슬롯 배정: STRONG 2팀을 슬롯 0/4(반대 사이드)에 고정, 나머지는 셔플 */
    private List<Long> drawSlots() {
        List<Team> teams = teamRepository.findAll();
        if (teams.size() < 8) throw BusinessException.conflict("SEED_MISSING", "팀 seed 데이터가 부족합니다.");

        List<Long> strong = teams.stream().filter(t -> t.getGrade() == TeamGrade.STRONG)
                .map(Team::getId).limit(2).toList();
        List<Long> others = new ArrayList<>(teams.stream().map(Team::getId)
                .filter(id -> !strong.contains(id)).toList());
        Collections.shuffle(others);

        List<Long> slots = new ArrayList<>(Collections.nCopies(8, null));
        int otherIdx = 0;
        for (int i = 0; i < 8; i++) {
            if (i == 0 && !strong.isEmpty()) slots.set(i, strong.get(0));
            else if (i == 4 && strong.size() > 1) slots.set(i, strong.get(1));
            else slots.set(i, others.get(otherIdx++));
        }
        return slots;
    }

    @Transactional(readOnly = true)
    public SaveGameResponse get(UUID userId, Long saveGameId) {
        SaveGame saveGame = requireOwned(userId, saveGameId);
        Team team = teamRepository.findById(saveGame.getTeamId())
                .orElseThrow(() -> BusinessException.notFound("팀"));
        Competition competition = competitionRepository.findBySaveGameId(saveGameId)
                .orElseThrow(() -> BusinessException.notFound("대회"));
        return SaveGameResponse.of(saveGame, team, competition);
    }

    @Transactional(readOnly = true)
    public List<SaveGameResponse> listByUser(UUID userId) {
        return saveGameRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sg -> get(userId, sg.getId()))
                .toList();
    }

    /** 저장 게임 삭제 (소유자만). FK 순서대로 자식(이벤트/결과/전술/경기/대회) 먼저 제거 */
    @Transactional
    public void delete(UUID userId, Long saveGameId) {
        SaveGame saveGame = requireOwned(userId, saveGameId);
        competitionRepository.findBySaveGameId(saveGameId).ifPresent(competition -> {
            List<Match> matches = matchRepository
                    .findByCompetitionIdOrderByRoundAscMatchNoAsc(competition.getId());
            List<Long> matchIds = matches.stream().map(Match::getId).toList();
            if (!matchIds.isEmpty()) {
                matchEventRepository.deleteByMatchIdIn(matchIds);
                matchResultRepository.deleteByMatchIdIn(matchIds);
                tacticRepository.deleteByMatchIdIn(matchIds);
            }
            matchRepository.deleteAll(matches);
            competitionRepository.delete(competition);
        });
        saveGameRepository.delete(saveGame);
    }

    /** 사용자 팀의 다음(미종료) 경기 */
    @Transactional(readOnly = true)
    public NextMatchResponse nextMatch(UUID userId, Long saveGameId) {
        SaveGame saveGame = requireOwned(userId, saveGameId);
        Competition competition = competitionRepository.findBySaveGameId(saveGameId)
                .orElseThrow(() -> BusinessException.notFound("대회"));

        return matchRepository.findByCompetitionIdOrderByRoundAscMatchNoAsc(competition.getId()).stream()
                .filter(m -> m.isUserMatch() && m.getStatus() != MatchStatus.FINISHED)
                .min(Comparator.comparingInt(m -> m.getRound().ordinal()))
                .map(m -> new NextMatchResponse(saveGame.getStatus().name(), true,
                        new NextMatchResponse.NextMatch(
                                m.getId(), m.getRound().name(), m.getRound().getLabel(),
                                m.getStatus().name(),
                                brief(m.getHomeTeamId()), brief(m.getAwayTeamId()),
                                m.getHomeTeamId().equals(saveGame.getTeamId()),
                                tacticRepository.findByMatchIdAndTeamId(m.getId(), saveGame.getTeamId())
                                        .isPresent())))
                .orElseGet(() -> new NextMatchResponse(saveGame.getStatus().name(), false, null));
    }

    private TeamBrief brief(Long teamId) {
        return teamRepository.findById(teamId).map(TeamBrief::from)
                .orElse(new TeamBrief(teamId, "팀 " + teamId, "?"));
    }

    /** 저장 게임 조회 + 소유권 검증 (남의 게임이면 403) */
    private SaveGame requireOwned(UUID userId, Long saveGameId) {
        SaveGame saveGame = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> BusinessException.notFound("저장 게임"));
        if (!saveGame.getUserId().equals(userId)) {
            throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "FORBIDDEN", "이 저장 게임에 대한 권한이 없습니다.");
        }
        return saveGame;
    }
}
