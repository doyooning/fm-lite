package com.fmlite.competition;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.Match;
import com.fmlite.match.MatchRepository;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.match.simulation.AiMatchRunner;
import com.fmlite.savegame.SaveGame;
import com.fmlite.savegame.SaveGameRepository;
import com.fmlite.savegame.SaveGameStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 토너먼트 진행 관리: 사용자 경기 종료 후 호출.
 * 같은 라운드의 AI vs AI 경기를 자동 시뮬레이션하고 다음 라운드 대진을 생성한다.
 * 사용자가 탈락하면 남은 대회를 끝까지 자동 진행한다.
 */
@Service
@RequiredArgsConstructor
public class TournamentProgressService {

    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final SaveGameRepository saveGameRepository;
    private final AiMatchRunner aiMatchRunner;

    @Transactional
    public void progress(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> BusinessException.notFound("대회"));
        SaveGame saveGame = saveGameRepository.findById(competition.getSaveGameId())
                .orElseThrow(() -> BusinessException.notFound("저장 게임"));

        while (competition.getCurrentRound() != Round.FINISHED) {
            Round round = competition.getCurrentRound();
            List<Match> matches = matchRepository
                    .findByCompetitionIdAndRoundOrderByMatchNoAsc(competitionId, round);

            // 사용자 경기가 아직 진행 전/중이면 여기서 멈추고 사용자 진행을 기다린다
            boolean userPending = matches.stream()
                    .anyMatch(m -> m.isUserMatch() && m.getStatus() != MatchStatus.FINISHED);
            if (userPending) return;

            // 남은 AI 경기 자동 시뮬레이션
            matches.stream()
                    .filter(m -> m.getStatus() != MatchStatus.FINISHED)
                    .forEach(aiMatchRunner::simulate);

            List<Long> winners = winnersOf(matches);

            if (round == Round.FINAL) {
                Long champion = winners.get(0);
                competition.finish(champion);
                if (saveGame.getStatus() == SaveGameStatus.IN_PROGRESS) {
                    saveGame.finish(champion.equals(saveGame.getTeamId())
                            ? SaveGameStatus.CHAMPION : SaveGameStatus.ELIMINATED);
                }
                return;
            }

            // 사용자 탈락 확정
            if (saveGame.getStatus() == SaveGameStatus.IN_PROGRESS
                    && !winners.contains(saveGame.getTeamId())) {
                saveGame.finish(SaveGameStatus.ELIMINATED);
            }

            createNextRound(competition, round.next(), winners, saveGame.getTeamId());
            competition.advanceTo(round.next());
        }
    }

    private List<Long> winnersOf(List<Match> matches) {
        List<Long> winners = new ArrayList<>();
        matches.stream()
                .sorted(Comparator.comparingInt(Match::getMatchNo))
                .forEach(m -> winners.add(matchResultRepository.findByMatchId(m.getId())
                        .orElseThrow(() -> BusinessException.notFound("경기 결과"))
                        .getWinnerTeamId()));
        return winners;
    }

    private void createNextRound(Competition competition, Round nextRound,
                                 List<Long> winners, Long userTeamId) {
        for (int i = 0; i < winners.size() / 2; i++) {
            Long home = winners.get(i * 2);
            Long away = winners.get(i * 2 + 1);
            boolean userMatch = home.equals(userTeamId) || away.equals(userTeamId);
            matchRepository.save(new Match(competition.getId(), nextRound, i + 1, home, away, userMatch));
        }
    }
}
