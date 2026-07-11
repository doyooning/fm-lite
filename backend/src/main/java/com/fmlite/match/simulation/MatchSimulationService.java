package com.fmlite.match.simulation;

import com.fmlite.common.exception.BusinessException;
import com.fmlite.competition.TournamentProgressService;
import com.fmlite.match.Match;
import com.fmlite.match.MatchAccess;
import com.fmlite.match.MatchAccess.MatchContext;
import com.fmlite.match.MatchStatus;
import com.fmlite.match.dto.ChoiceRequest;
import com.fmlite.match.dto.MatchEventResponse;
import com.fmlite.match.dto.MatchProgressResponse;
import com.fmlite.match.event.ChoiceOption;
import com.fmlite.match.event.MatchEvent;
import com.fmlite.match.event.MatchEventRepository;
import com.fmlite.match.event.MatchEventType;
import com.fmlite.match.simulation.MatchEngine.EngineContext;
import com.fmlite.match.simulation.model.ActiveEffect;
import com.fmlite.match.simulation.model.EventDraft;
import com.fmlite.match.simulation.model.SimulationState;
import com.fmlite.match.simulation.model.TeamSimState;
import com.fmlite.match.tactic.Tactic;
import com.fmlite.match.tactic.TacticRepository;
import com.fmlite.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 사용자 경기 진행 오케스트레이터: 시작 → (선택지 일시정지 ↔ 재개) → 종료/토너먼트 진행 */
@Service
@RequiredArgsConstructor
public class MatchSimulationService {

    private final MatchAccess matchAccess;
    private final MatchEngine engine;
    private final MatchSupport matchSupport;
    private final MatchEventWriter eventWriter;
    private final MatchFinalizer finalizer;
    private final MatchChoiceService choiceService;
    private final MatchEventGenerator eventGenerator;
    private final TacticRepository tacticRepository;
    private final MatchEventRepository matchEventRepository;
    private final AiMatchRunner aiMatchRunner;
    private final TournamentProgressService tournamentProgressService;

    @Transactional
    public MatchProgressResponse start(Long matchId, UUID userId) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        Match match = ctx.match();
        if (!match.isUserMatch()) {
            throw BusinessException.conflict("NOT_USER_MATCH", "사용자 팀의 경기가 아닙니다.");
        }

        // 멱등성: 이미 시작/종료된 경기는 전체 이벤트 로그를 그대로 반환
        if (match.getStatus() != MatchStatus.SCHEDULED) {
            return snapshot(match, matchEventRepository.findByMatchIdOrderBySeqAsc(matchId).stream()
                    .map(MatchEventResponse::from).toList());
        }

        initialize(match, ctx.userTeamId());
        return runAndPersist(match, new ArrayList<>());
    }

    @Transactional
    public MatchProgressResponse submitChoice(Long matchId, UUID userId, ChoiceRequest request) {
        MatchContext ctx = matchAccess.userContext(matchId, userId);
        Match match = ctx.match();
        if (match.getStatus() != MatchStatus.WAITING_CHOICE) {
            throw BusinessException.conflict("NOT_WAITING_CHOICE", "선택지를 기다리는 상태가 아닙니다.");
        }

        MatchEvent event = matchEventRepository.findById(request.eventId())
                .filter(e -> e.getMatchId().equals(matchId))
                .orElseThrow(() -> BusinessException.notFound("선택지 이벤트"));
        if (!event.isRequiresChoice() || event.getSelectedChoiceId() != null) {
            throw BusinessException.conflict("CHOICE_ALREADY_RESOLVED", "이미 처리된 선택지입니다.");
        }
        boolean validOption = event.getChoiceOptions() != null && event.getChoiceOptions().stream()
                .map(ChoiceOption::getId).anyMatch(id -> id.equals(request.choiceId()));
        if (!validOption) {
            throw BusinessException.badRequest("INVALID_CHOICE", "선택할 수 없는 옵션입니다.");
        }

        event.selectChoice(request.choiceId());

        SimulationState state = match.getSimulationState();
        boolean userIsHome = !state.getHome().isAiControlled();
        TeamSimState userSide = state.side(userIsHome);
        var spec = choiceService.spec(request.choiceId());
        state.getEffects().add(new ActiveEffect(request.choiceId(),
                userIsHome ? "HOME" : "AWAY", spec.durationTicks()));
        userSide.setStamina(Math.max(0.4, userSide.getStamina() - spec.staminaCost()));
        state.setWaitingChoice(false);
        match.updateState(state, MatchStatus.IN_PROGRESS);

        List<MatchEventResponse> events = eventWriter.persist(match, state, List.of(
                EventDraft.info(state.minuteNow(), MatchEventType.COACH_DECISION,
                        eventGenerator.coachDecision(choiceService.labelOf(request.choiceId())))));
        return runAndPersist(match, events);
    }

    @Transactional(readOnly = true)
    public MatchProgressResponse getEvents(Long matchId, Integer afterSeq) {
        Match match = matchAccess.context(matchId).match();
        List<MatchEvent> events = afterSeq == null
                ? matchEventRepository.findByMatchIdOrderBySeqAsc(matchId)
                : matchEventRepository.findByMatchIdAndSeqGreaterThanOrderBySeqAsc(matchId, afterSeq);
        return snapshot(match, events.stream().map(MatchEventResponse::from).toList());
    }

    private void initialize(Match match, Long userTeamId) {
        Tactic userTactic = tacticRepository.findByMatchIdAndTeamId(match.getId(), userTeamId)
                .orElseThrow(() -> BusinessException.conflict("TACTIC_NOT_SET",
                        "경기 시작 전에 전술을 설정해야 합니다."));
        Long opponentId = match.opponentOf(userTeamId);
        Tactic opponentTactic = tacticRepository.findByMatchIdAndTeamId(match.getId(), opponentId)
                .orElseGet(() -> tacticRepository.save(aiMatchRunner.buildAiTactic(match.getId(), opponentId)));

        Map<Long, Tactic> tactics = new HashMap<>();
        tactics.put(userTeamId, userTactic);
        tactics.put(opponentId, opponentTactic);

        Map<Long, List<Player>> squads = matchSupport.squads(match);
        SimulationState state = matchSupport.initState(match, tactics, userTeamId, squads);
        match.updateState(state, MatchStatus.IN_PROGRESS);
    }

    private MatchProgressResponse runAndPersist(Match match, List<MatchEventResponse> collected) {
        Map<Long, List<Player>> squads = matchSupport.squads(match);
        Map<Long, String> names = matchSupport.teamNames(match);
        SimulationState state = match.getSimulationState();

        var outcome = engine.run(new EngineContext(match.getRound(), squads, names, state));
        collected.addAll(eventWriter.persist(match, state, outcome.events()));

        if (outcome.regularTimeFinished()) {
            collected.addAll(finalizer.finalizeMatch(match, state, squads, names));
            tournamentProgressService.progress(match.getCompetitionId());
            return snapshot(match, collected);
        }

        match.updateState(state, MatchStatus.WAITING_CHOICE);
        return snapshot(match, collected);
    }

    private MatchProgressResponse snapshot(Match match, List<MatchEventResponse> events) {
        SimulationState st = match.getSimulationState();
        int home = st == null ? 0 : st.getHomeScore();
        int away = st == null ? 0 : st.getAwayScore();
        int minute = match.getStatus() == MatchStatus.FINISHED ? 90 : (st == null ? 0 : st.minuteNow());
        return new MatchProgressResponse(match.getStatus().name(),
                new MatchProgressResponse.Score(home, away), minute, events);
    }
}
