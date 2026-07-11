# 07. 구현 우선순위 & 확장 기능

## 구현 로드맵 (MVP 8단계)

각 단계는 **끝나면 확인 가능한 결과물**이 있도록 나눴다. 5단계까지가 게임의 심장이므로
프론트 연동(7단계) 전에 백엔드를 테스트 코드로 검증해 두는 것이 핵심.

### 1단계 — 프로젝트 셋업 + DB/Seed
- 모노레포 생성, Spring Boot 프로젝트 초기화 (Web, JPA, Flyway, Validation)
- Supabase 프로젝트 생성, 연결 확인 (HikariCP pool size 제한)
- `V1__init.sql` (전체 스키마), `V2__seed.sql` (8팀 × 18명, 능력치·특성 생성 스크립트로 1회 생성 후 고정)
- ✅ 완료 기준: DB에 팀 8개/선수 144명 조회됨

### 2단계 — 팀/선수 조회 API
- `GET /teams`, `/teams/{id}`, `/teams/{id}/players`
- 공통 응답 래퍼, 전역 예외 처리
- ✅ 완료 기준: Postman/httpie로 3개 API 정상 응답

### 3단계 — 새 게임 / 대회 생성
- `POST /users`, `POST /save-games` (SaveGame + Competition + QF 대진 생성, STRONG 팀 시드 배정)
- `GET /save-games/{id}`, `/next-match`, `GET /competitions/{id}` (대진표)
- ✅ 완료 기준: 새 게임 생성 시 8강 4경기가 올바른 대진으로 생성됨

### 4단계 — 전술 설정
- `GET/PUT /matches/{id}/tactics/me`, enum 검증
- AI 팀 기본 전술 생성 로직 (등급/콘셉트 테이블)
- 상대 분석 API (`/opponent-analysis`) — 전력 계산 + 규칙 기반 강약점 텍스트
- ✅ 완료 기준: 전술 저장·조회, 상대 분석 응답 확인

### 5단계 — 단판 경기 시뮬레이션 ★ 최대 작업량
- 시뮬레이션 엔진 (TeamPowerCalculator → TacticEvaluation → 틱 루프 → 이벤트 생성 → 결과 계산)
- 선택지 일시정지/재개 (simulation_state 저장·복원)
- OpponentAiService 규칙 5종
- `POST /start`, `POST /choices`, `GET /events`, `GET /result`
- **몬테카를로 테스트**: 1,000경기 평균 득점 2.5~3.0, 강팀 vs 약팀 승률 70~80% 튜닝
- ✅ 완료 기준: API만으로 경기 1판을 시작→선택→종료까지 진행 가능

### 6단계 — 토너먼트 진행
- TournamentProgressService: AI vs AI 자동 시뮬레이션, 다음 라운드 생성, 우승/탈락 처리
- ✅ 완료 기준: API만으로 8강→우승까지 풀 플로우 완주 가능

### 7단계 — 프론트 연동
- types/ + lib/api/ 먼저 작성 (API 명세와 1:1)
- 화면 9종을 플로우 순서대로: 홈 → 팀 선택 → 허브 → 선수단 → 상대 분석 → 전술 → **경기 진행** → 결과 → 대진표
- ✅ 완료 기준: 브라우저에서 새 게임 → 우승/탈락까지 완주 가능

### 8단계 — UI 개선 + 배포
- 경기 진행 연출 (이벤트 타이핑 딜레이, 골 강조, 선택지 카운트다운 등)
- 반응형/다크 테마 다듬기, 로딩·에러 상태
- Vercel(프론트) + Railway 등(백엔드) 배포, CORS/환경변수 정리
- ✅ 완료 기준: 배포 URL에서 지인이 플레이 가능

## 확장 기능 (MVP 이후 백로그)

| 우선순위 | 기능 | 설계 메모 |
|---|---|---|
| 높음 | **AI 경기 리포트** | 경기 후 이벤트 로그 + 통계를 LLM에 전달해 분석 리포트 생성. 판정 로직 무변경, `match.analysis`에 서비스 추가만 |
| 높음 | **AI 상대 분석/전술 추천 문장** | 현재 규칙 기반 JSON을 LLM 프롬프트 입력으로 재사용 |
| 높음 | **경기 간 체력/컨디션 이월** | `save_game_players` 스냅샷 테이블 도입 (save_game_id, player_id, condition, fatigue). 선수 성장의 기반이 됨 |
| 중간 | **선발 라인업/교체 수동 편집** | Tactic에 lineup jsonb 추가, 경기 중 교체 선택지 |
| 중간 | **부상/징계** | MatchEvent에 INJURY/CARD 타입 추가, save_game_players에 상태 기록 |
| 중간 | **선수 성장** | 경기 출전·평점 기반 능력치 상승 (save_game_players 위에 구현) |
| 중간 | **시즌 모드 (리그전)** | Competition.type=LEAGUE, 승점 테이블 추가. Match 구조는 재사용 |
| 중간 | **인증 (Spring Security + JWT)** | users에 email/password 추가, X-User-Id → Bearer 토큰 교체 |
| 낮음 | **훈련 시스템** | 경기 사이 훈련 선택 → 컨디션/능력치 버프 |
| 낮음 | **이적 시장** | 대회 사이 선수 영입 (예산 개념 필요) |
| 낮음 | **관리자 데이터 관리** | 팀/선수/밸런스 상수 CRUD 어드민 (Next.js /admin 라우트) |
| 낮음 | **경기 스토리/해설 LLM 생성** | 템플릿 텍스트 계층을 LLM 스트리밍으로 교체 |

## 리스크 & 미리 정한 대응

| 리스크 | 대응 |
|---|---|
| 시뮬레이션 밸런스 붕괴 (0골 경기, 약팀 우승 남발) | 5단계 몬테카를로 테스트를 CI에 포함, 상수는 한 클래스(`SimulationConstants`)에 모아 튜닝 |
| 경기 진행 중 새로고침/이탈 | 모든 상태를 DB(simulation_state)에 저장, GET /events로 복구 — 설계에 반영됨 |
| Supabase 무료 티어 커넥션 제한 | HikariCP max 5~10, transaction pooler 사용 |
| 백엔드 콜드 스타트 (무료 호스팅) | MVP 허용. 문제 시 헬스체크 핑 또는 유료 티어 |
