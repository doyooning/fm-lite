package com.fmlite.savegame;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.competition.Competition;
import com.fmlite.competition.CompetitionRepository;
import com.fmlite.match.Match;
import com.fmlite.match.MatchRepository;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.dto.TeamBrief;
import com.fmlite.match.tactic.TacticRepository;
import com.fmlite.savegame.dto.NextMatchResponse;
import com.fmlite.savegame.dto.SaveGameResponse;
import com.fmlite.competition.Round;
import com.fmlite.team.Team;
import com.fmlite.team.TeamGrade;
import com.fmlite.team.TeamRepository;
import com.fmlite.user.User;
import com.fmlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
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

    private final SaveGameRepository saveGameRepository;
    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TacticRepository tacticRepository;
    private final UserRepository userRepository;

    /** 새 게임 시작: SaveGame + Competition + 8강 대진 생성 (강팀 2팀은 반대 사이드 시드 배정) */
    @Transactional
    public SaveGameResponse create(UUID userId, Long teamId) {
        // 익명 사용자: 클라이언트가 보관한 id 가 DB 초기화 등으로 없으면 그 id 로 자동 생성(자기치유).
        // save_games.user_id FK 를 만족시키려면 SaveGame INSERT 전에 users 행이 확정돼야 하므로 flush.
        if (!userRepository.existsById(userId)) {
            userRepository.saveAndFlush(new User(userId, "감독"));
        }
        Team userTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("팀"));

        SaveGame saveGame = saveGameRepository.save(new SaveGame(userId, teamId));
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
    public SaveGameResponse get(Long saveGameId) {
        SaveGame saveGame = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> BusinessException.notFound("저장 게임"));
        Team team = teamRepository.findById(saveGame.getTeamId())
                .orElseThrow(() -> BusinessException.notFound("팀"));
        Competition competition = competitionRepository.findBySaveGameId(saveGameId)
                .orElseThrow(() -> BusinessException.notFound("대회"));
        return SaveGameResponse.of(saveGame, team, competition);
    }

    @Transactional(readOnly = true)
    public List<SaveGameResponse> listByUser(UUID userId) {
        return saveGameRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sg -> get(sg.getId()))
                .toList();
    }

    /** 사용자 팀의 다음(미종료) 경기 */
    @Transactional(readOnly = true)
    public NextMatchResponse nextMatch(Long saveGameId) {
        SaveGame saveGame = saveGameRepository.findById(saveGameId)
                .orElseThrow(() -> BusinessException.notFound("저장 게임"));
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
}
