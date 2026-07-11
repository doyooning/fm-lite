package com.fmlite.competition;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.competition.dto.BracketResponse;
import com.fmlite.competition.dto.BracketResponse.BracketMatch;
import com.fmlite.competition.dto.BracketResponse.RoundBracket;
import com.fmlite.match.Match;
import com.fmlite.match.MatchRepository;
import com.fmlite.match.dto.TeamBrief;
import com.fmlite.match.result.MatchResult;
import com.fmlite.match.result.MatchResultRepository;
import com.fmlite.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final TeamRepository teamRepository;

    public BracketResponse bracket(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> BusinessException.notFound("대회"));
        List<Match> matches = matchRepository.findByCompetitionIdOrderByRoundAscMatchNoAsc(competitionId);
        Map<Long, MatchResult> results = matchResultRepository
                .findByMatchIdIn(matches.stream().map(Match::getId).toList()).stream()
                .collect(Collectors.toMap(MatchResult::getMatchId, Function.identity()));

        List<RoundBracket> rounds = new ArrayList<>();
        for (Round round : List.of(Round.QF, Round.SF, Round.FINAL)) {
            List<BracketMatch> roundMatches = matches.stream()
                    .filter(m -> m.getRound() == round)
                    .sorted(Comparator.comparingInt(Match::getMatchNo))
                    .map(m -> toBracketMatch(m, Optional.ofNullable(results.get(m.getId()))))
                    .toList();
            rounds.add(new RoundBracket(round.name(), round.getLabel(), roundMatches));
        }

        return new BracketResponse(competition.getId(), competition.getName(),
                competition.getCurrentRound().name(), competition.getCurrentRound().getLabel(),
                competition.getWinnerTeamId(), rounds);
    }

    private BracketMatch toBracketMatch(Match match, Optional<MatchResult> result) {
        return new BracketMatch(
                match.getId(), match.getMatchNo(),
                brief(match.getHomeTeamId()), brief(match.getAwayTeamId()),
                match.getStatus().name(), match.isUserMatch(),
                result.map(MatchResult::getHomeScore).orElse(null),
                result.map(MatchResult::getAwayScore).orElse(null),
                result.map(MatchResult::getPenaltyHomeScore).orElse(null),
                result.map(MatchResult::getPenaltyAwayScore).orElse(null),
                result.map(MatchResult::getWinnerTeamId).orElse(null));
    }

    private TeamBrief brief(Long teamId) {
        return teamRepository.findById(teamId).map(TeamBrief::from)
                .orElse(new TeamBrief(teamId, "팀 " + teamId, "?"));
    }
}
