# AI 경기는 결과·통계만 저장 — 구현 지시

## 컨텍스트

- 저장소 루트: `C:\claude_projects\fm-lite` (Windows)
- 백엔드: Spring Boot 3.5.6 / Java 17 / Gradle / JPA / Flyway, 패키지 `com.fmlite`
- 프론트: Next.js 16 App Router / TypeScript / Tailwind v4 (이번 작업에서는 건드리지 않는다)
- **먼저 읽을 것**: 루트 `AGENTS.md` (코드 규약), 설계 근거는 `docs/specs/001-ai-match-result-only.md`

배경: 사용자가 참여하지 않는 AI vs AI 경기도 지금은 분 단위 이벤트를 한 건씩 DB에 INSERT한다.
이 이벤트는 화면 어디에서도 읽지 않는데, 사용자의 75분 선택 요청 하나에 최대 6경기분(약 84회)의
DB 왕복이 붙어 응답이 88초까지 늘어난다. **엔진은 그대로 돌리고 저장만 건너뛴다.**

## 작업 범위

AI vs AI 경기에서 `match_events` 행을 만들지 않는다.
결과·통계(`match_results`)와 진행 스냅샷(`matches.simulation_state`)은 지금처럼 저장한다.

변경 파일은 **아래 2개뿐**이다. 그 밖의 파일은 건드리지 않는다.

## 하지 않을 것

- 사용자 경기의 이벤트 저장 방식 변경 — 지금 그대로 전부 저장한다
- `MatchEventWriter` 수정 — "저장하는 방법"이 아니라 "저장할지 여부"만 바뀌는 변경이다
- 시뮬레이션 판정 로직·상수(`SimulationConstants`, `MatchEngine`) 변경 — 밸런스가 바뀌면 안 된다
- 토너먼트 자동 진행을 비동기(@Async)로 빼는 것 — 별도 작업이다
- 이벤트 배치 INSERT / `GenerationType.SEQUENCE` 전환 — 별도 작업이다
- DB 마이그레이션 추가 — 이번 변경에 스키마 변경은 없다
- 프론트엔드 수정
- 범위 밖 리팩터링·포맷팅·의존성 추가

## 변경 사항

### 1. `backend/src/main/java/com/fmlite/match/simulation/AiMatchRunner.java` (수정)

`simulate(Match match)` 에서 이벤트 저장 호출을 제거한다.

- `eventWriter.persist(match, state, outcome.events());` 한 줄 삭제
- 그 결과 쓰이지 않게 되는 `private final MatchEventWriter eventWriter;` 필드와
  해당 `import` 도 함께 삭제 (Lombok `@RequiredArgsConstructor`라 생성자는 자동 정리된다)
- `engine.run(...)` 호출과 `finalizer.finalizeMatch(...)` 호출은 **그대로 둔다.**
  통계는 이벤트가 아니라 엔진이 갱신한 `state`(`shots`, `shotsOnTarget`, `domSumHome`, `goals`)에서
  계산되므로, 엔진을 끝까지 돌려야 결과가 맞는다.

### 2. `backend/src/main/java/com/fmlite/match/simulation/MatchFinalizer.java` (수정)

`finalizeMatch(...)` 에서 종료 이벤트(`FULL_TIME`, `PENALTY_SHOOTOUT`)를
**사용자 경기일 때만** 저장하도록 분기한다.

- 판단 기준은 `match.isUserMatch()` **하나만** 쓴다.
  `SimulationState`의 `isAiControlled()`로 판단하거나, 메서드 시그니처에
  `boolean persistEvents` 같은 플래그를 추가하지 말 것 — 호출부가 둘로 갈리면 규칙이 흩어진다.
- `match.isUserMatch()`가 `false`면 `eventWriter.persist(...)`를 호출하지 않고 빈 리스트를 반환한다.
- `matchResultRepository.save(new MatchResult(...))` 와 `match.updateState(state, MatchStatus.FINISHED)` 는
  **사용자·AI 경기 모두 종전대로** 수행한다. 승부차기 스코어는 `MatchResult`에 남으므로
  이벤트가 없어도 대진표·결과 화면에 영향이 없다.
- 메서드 시그니처와 반환 타입(`List<MatchEventResponse>`)은 유지한다.

## 완료 기준

- [ ] AI vs AI 경기가 끝난 뒤 `match_events`에 해당 `match_id` 행이 **0건**
- [ ] 사용자 경기의 이벤트는 종전과 동일하게 저장된다 (개수·순서 변화 없음)
- [ ] 승부차기로 끝난 AI 경기의 `penaltyHomeScore` / `penaltyAwayScore`가 `match_results`에 저장된다
- [ ] 대진표(`GET /competitions/{id}`)에 AI 경기의 스코어·승자·상태가 정상 표시된다
- [ ] `MatchEngineBalanceTest` 통과 (평균 득점 2.5~3.0, 강팀 대 약팀 승률 70~80%)
- [ ] 변경 파일이 위 2개 외에 없다

## 자체 검증 (구현 후 직접 실행)

```bash
cd backend && ./gradlew test
```

기대: 전부 통과. 특히 `MatchEngineBalanceTest`의 몬테카를로 결과가 기존 범위를 유지해야 한다
(판정 로직을 건드리지 않았으므로 변해서는 안 된다). 실패하면 고친 뒤 다시 실행할 것.

> `tools/e2e-test.mjs` 는 **지금 실행할 수 없다** — `pg` 패키지를 import 하는데 저장소에
> `package.json`/`node_modules`가 없어 `ERR_MODULE_NOT_FOUND`가 난다. 이번 작업 범위가 아니니
> 해결하려 하지 말고 건너뛸 것. 별도로 처리한다.

프론트는 변경이 없으므로 타입체크·린트는 생략해도 된다.

## 보고

- 변경한 파일 목록
- `./gradlew test` 결과 (특히 `MatchEngineBalanceTest`의 평균 득점·승률 수치)
- 명세와 다르게 구현한 부분이 있으면 그 이유
