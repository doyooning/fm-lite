# 001. AI 경기는 결과·통계만 저장

- 상태: 설계 완료
- 관련 문서: [docs/05-simulation-design.md](../05-simulation-design.md), [docs/03-database-erd.md](../03-database-erd.md)

## 목표

사용자가 참여하지 않는 **AI vs AI 경기**의 분 단위 이벤트를 DB에 저장하지 않는다.
스코어·승부차기·통계(`match_results`)와 진행 스냅샷(`matches.simulation_state`)만 남긴다.

현재 AI 경기 한 판은 12~18건의 이벤트를 **한 건씩** INSERT한다(`MatchEventWriter:28`).
DB가 원격이라 왕복 지연이 그대로 곱해져, 사용자의 75분 선택 요청 하나에 최대 6경기 ×
약 15건 = 84회의 불필요한 왕복이 붙는다(실측: 75분 선택 응답 88.4초, 다른 선택지는 4~5초).
그런데 이 이벤트들은 **아무도 읽지 않는다** — 프론트는 지금 보고 있는 경기의 이벤트만
조회한다(`frontend/src/app/game/[saveGameId]/match/[matchId]/page.tsx:64`).

향후 '다른 팀 경기 실시간 조회' 기능은 저장된 이벤트를 읽는 대신 **seed로 경기를 재현**해
제공한다 (아래 "향후 확장의 근거" 참조).

## 비목표

- 사용자 경기의 이벤트 저장 방식 변경 — 지금 그대로 전부 저장한다
- 토너먼트 자동 진행을 비동기로 분리하는 것 — 별도 작업
- 이벤트 배치 INSERT / `GenerationType.SEQUENCE` 전환 — 별도 작업
- '다른 팀 경기 실시간 조회' 기능 구현 자체
- 시뮬레이션 판정 로직·상수 변경 (밸런스에 영향을 주면 안 된다)

## DB 변경

**없음.** 스키마는 그대로이고, `match_events`에 AI 경기 행이 더 이상 쌓이지 않을 뿐이다.

기존에 쌓인 AI 경기 이벤트 행 정리는 선택 사항이다. 리전 이전으로 새 DB를 쓰면 자연히 사라지므로
별도 마이그레이션을 만들지 않는다. 같은 DB를 계속 쓴다면 아래를 수동 실행할 수 있다.

```sql
delete from match_events e
 using matches m
 where e.match_id = m.id and m.is_user_match = false;
```

## API 계약

**변경 없음.** `GET /matches/{id}/events` 는 그대로 두고, AI 경기 id로 호출하면 빈 배열을 반환한다
(현재 화면에서 그런 호출은 발생하지 않는다).

## 백엔드 변경

| 파일 | 신규/수정 | 내용 |
|---|---|---|
| `match/simulation/AiMatchRunner.java` | 수정 | `simulate()`에서 `eventWriter.persist(match, state, outcome.events())` 호출 제거. 더 이상 쓰지 않는 `MatchEventWriter` 의존성 필드도 삭제. 엔진 실행(`engine.run`)과 `finalizer.finalizeMatch` 호출은 그대로 둔다 — 통계는 엔진이 갱신한 `state`에서 나온다. |
| `match/simulation/MatchFinalizer.java` | 수정 | `finalizeMatch(...)`가 `match.isUserMatch()`가 `false`면 `outcome.extraEvents()`를 저장하지 않고 빈 리스트를 반환하도록 분기. `MatchResult` 저장과 `match.updateState(state, FINISHED)`는 사용자·AI 경기 모두 종전대로 수행한다. |

설계 의도:

- 저장 여부의 판단 기준을 `match.is_user_match` **한 곳**으로 모은다. 메서드 시그니처에
  `boolean persistEvents` 같은 플래그를 추가하지 말 것 — 호출부가 둘로 갈리면 규칙이 흩어진다.
- `MatchEventWriter`는 손대지 않는다. "저장하는 방법"이 아니라 "저장할지 여부"만 바뀌는 변경이다.
- 승부차기 이벤트(`PENALTY_SHOOTOUT`)도 AI 경기에서는 저장하지 않는다. 결과는
  `match_results.penalty_home_score / penalty_away_score`에 남으므로 대진표·결과 표시에 문제가 없다.

## 프론트엔드 변경

**없음.** 프론트는 현재 보고 있는 경기(= 사용자 경기)의 이벤트만 조회한다.

## 시뮬레이션 영향

**판정 로직 변경 없음.** 엔진은 그대로 돌고, 저장 단계만 건너뛴다.
`MatchResultCalculator`가 만드는 통계는 이벤트가 아니라 `SimulationState`의
`shots` / `shotsOnTarget` / `domSumHome` / `goals`에서 계산되므로 영향을 받지 않는다.

`MatchEngineBalanceTest`(몬테카를로) 결과는 변하지 않아야 한다 — 평균 득점 2.5~3.0,
강팀 대 약팀 승률 70~80%.

## 향후 확장의 근거 — '다른 팀 경기 실시간 조회'

이벤트를 지워도 나중에 같은 경기를 **재생성**할 수 있다. 근거는 다음과 같다.

| 재현에 필요한 것 | 어디에 남는가 |
|---|---|
| 난수 seed | `matches.simulation_state.seed` — 초기화 후 변경되지 않는다 |
| 양 팀 초기 전술 | `tactics` 테이블 — 경기 중 AI의 전술 변경은 `TeamSimState`(메모리/스냅샷)만 바꾸고 이 테이블은 건드리지 않는다 |
| 선발 라인업 | `LineupSelector`가 결정론적으로 선발. 사용자 지정 라인업은 `tactics.lineup` |
| 선수 컨디션 | `MatchSupport.initState()`가 같은 seed(`new Random(seed)`)로 재생성 |
| 틱별 난수 | `new Random(seed ^ 7919 * (tick + 1))` — 틱 단위로 결정론적 |

즉 **저장된 seed + `tactics` + 시드 고정 마스터 데이터**로 `initState`를 다시 실행하면
같은 경기가 그대로 재생된다. 다만 이번 작업에서 그 경로를 만들지는 않는다. 기능을 설계할 때
다음 두 가지를 반드시 다뤄야 한다.

1. `MatchSupport.initState()`는 매번 `new Random().nextLong()`으로 새 seed를 만든다.
   재현하려면 **저장된 seed를 주입하는 경로**가 필요하다.
2. 엔진 로직이나 `SimulationConstants`를 튜닝하면 과거 경기의 재현 결과가 저장된 최종 스코어와
   달라질 수 있다. 불일치 시 **저장된 `match_results`를 정답으로 삼는** 규칙이 필요하다.

## 수용 기준

- [ ] AI vs AI 경기가 끝난 뒤 `match_events`에 해당 `match_id` 행이 **0건**
- [ ] 사용자 경기의 이벤트는 종전과 동일하게 저장된다 (개수·순서 변화 없음)
- [ ] 대진표(`GET /competitions/{id}`)에 AI 경기의 스코어·승자·상태가 정상 표시된다
- [ ] 승부차기로 끝난 AI 경기의 `penaltyHomeScore` / `penaltyAwayScore`가 `match_results`에 저장된다
- [ ] `MatchEngineBalanceTest` 통과 (평균 득점 2.5~3.0, 강팀 승률 70~80%)
- [ ] `tools/e2e-test.mjs` 통과 — 8강부터 우승/탈락까지 완주
- [ ] 75분 선택 응답시간이 변경 전 대비 유의미하게 줄어든다 (같은 DB 리전 기준)

## 검증 방법

```bash
cd backend && ./gradlew test
cd frontend && npx tsc --noEmit && npm run lint
node tools/e2e-test.mjs
```

이벤트가 실제로 저장되지 않는지 확인 (AI 경기 id 하나를 골라):

```bash
curl -s http://localhost:8080/api/v1/matches/<AI경기id>/events
# data.events 가 빈 배열이어야 한다
```

응답시간 비교 — 경기 하나를 API로 완주시키며 각 선택지 응답을 잰다.
변경 전 기준값: 시작 5.5초 / 30분 4.0초 / 하프타임 3.5초 / 55분 5.2초 / **75분 88.4초**.

## 함께 갱신할 문서

- [x] `docs/05-simulation-design.md` — AI 경기는 이벤트를 저장하지 않는다는 점, 재현 근거
- [x] `docs/03-database-erd.md` — `match_events`에 사용자 경기 행만 쌓인다는 주석
- [x] `docs/07-roadmap.md` — 확장 백로그에 '다른 팀 경기 실시간 조회' 항목 추가
