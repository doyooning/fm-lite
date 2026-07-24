package com.fmlite.profile;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.match.Match;
import com.fmlite.match.MatchRepository;
import com.fmlite.match.result.MatchResult;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.profile.dto.ProfileResponse;
import com.fmlite.savegame.SaveGame;
import com.fmlite.savegame.SaveGameRepository;
import com.fmlite.savegame.SaveGameStatus;
import com.fmlite.team.TeamGrade;
import com.fmlite.team.TeamRepository;
import com.fmlite.user.User;
import com.fmlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 프로필(우승 횟수·업적) 조회 및 업적 부여 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserAchievementRepository achievementRepository;
    private final SaveGameRepository saveGameRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자"));

        List<SaveGame> games = saveGameRepository.findByUserIdOrderByCreatedAtDesc(userId);
        var stats = new ProfileResponse.GameStats(
                games.size(),
                (int) games.stream().filter(g -> g.getStatus() == SaveGameStatus.IN_PROGRESS).count(),
                (int) games.stream().filter(g -> g.getStatus() == SaveGameStatus.CHAMPION).count(),
                (int) games.stream().filter(g -> g.getStatus() == SaveGameStatus.ELIMINATED).count());

        Map<Achievement, UserAchievement> achieved = achievementRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserAchievement::getCode, Function.identity(), (a, b) -> a));

        List<ProfileResponse.AchievementItem> items = new ArrayList<>();
        for (Achievement a : Achievement.values()) {
            UserAchievement ua = achieved.get(a);
            items.add(new ProfileResponse.AchievementItem(
                    a.name(), a.getLabel(), a.getDescription(), a.getIcon(),
                    ua != null, ua == null ? null : ua.getAchievedAt()));
        }

        return new ProfileResponse(user.getEmail(), user.getCreatedAt(),
                user.getChampionships(), stats, items);
    }

    /** 게임 생성 시 호출 */
    @Transactional
    public void onGameCreated(UUID userId) {
        grant(userId, Achievement.FIRST_GAME);
    }

    /** 우승 확정 시 호출: 우승 횟수 증가 + 관련 업적 부여 */
    @Transactional
    public void onChampionship(UUID userId, SaveGame saveGame, Long competitionId) {
        userRepository.findById(userId).ifPresent(User::addChampionship);
        int championships = userRepository.findById(userId).map(User::getChampionships).orElse(0);

        grant(userId, Achievement.FIRST_WIN);
        if (championships >= 3) grant(userId, Achievement.VETERAN);

        teamRepository.findById(saveGame.getTeamId()).ifPresent(team -> {
            if (team.getGrade() == TeamGrade.MID || team.getGrade() == TeamGrade.WEAK) {
                grant(userId, Achievement.UNDERDOG_WIN);
            }
        });

        if (concededZero(saveGame.getTeamId(), competitionId)) {
            grant(userId, Achievement.NO_CONCEDE);
        }
    }

    /** 해당 대회에서 사용자 팀이 정규시간 실점이 0인지 */
    private boolean concededZero(Long userTeamId, Long competitionId) {
        List<Match> userMatches = matchRepository
                .findByCompetitionIdOrderByRoundAscMatchNoAsc(competitionId).stream()
                .filter(Match::isUserMatch)
                .toList();
        if (userMatches.isEmpty()) return false;

        for (Match m : userMatches) {
            Optional<MatchResult> result = matchResultRepository.findByMatchId(m.getId());
            if (result.isEmpty()) return false;
            MatchResult r = result.get();
            int conceded = m.getHomeTeamId().equals(userTeamId) ? r.getAwayScore() : r.getHomeScore();
            if (conceded > 0) return false;
        }
        return true;
    }

    private void grant(UUID userId, Achievement achievement) {
        if (!achievementRepository.existsByUserIdAndCode(userId, achievement)) {
            achievementRepository.save(new UserAchievement(userId, achievement));
        }
    }
}
