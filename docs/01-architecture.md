# 01. 아키텍처 & 디렉터리 구조

## 시스템 아키텍처

```
[Browser]
   │  HTTPS
   ▼
[Vercel — Next.js]  ──(REST, JSON)──▶  [Spring Boot API 서버]
   (화면 렌더링, API 호출만 담당)            │  JDBC (connection pooler 경유)
                                          ▼
                                    [Supabase PostgreSQL]
                                    (관리형 PostgreSQL로만 사용.
                                     Supabase Auth/RLS/Edge Function 미사용)
```

- 게임 로직은 전부 Spring Boot에 존재. Next.js는 화면 + API 클라이언트 역할만.
- Supabase는 DB 호스팅 용도. 연결은 Supabase가 제공하는 **connection pooler(port 6543, transaction mode)** 사용 권장 (서버리스 아님이므로 direct 5432도 무방하나, 무료 티어 커넥션 수 제한 주의 → HikariCP `maximum-pool-size`를 5~10으로 제한).
- CORS: Spring Boot에서 Vercel 도메인 허용.

## 모노레포 최상위 구조

```
fm-lite/
├── frontend/          # Next.js
├── backend/           # Spring Boot
├── docs/              # 설계/기획 문서
├── .gitignore
└── README.md
```

## Frontend (Next.js, App Router)

```
frontend/
├── src/
│   ├── app/
│   │   ├── layout.tsx                  # 공통 레이아웃 (헤더, 폰트, 다크 테마)
│   │   ├── page.tsx                    # 홈 (새 게임 / 이어하기)
│   │   ├── new-game/
│   │   │   └── page.tsx                # 팀 선택
│   │   └── game/[saveGameId]/
│   │       ├── layout.tsx              # 게임 내 공통 레이아웃 (팀명, 대회 상태 바)
│   │       ├── page.tsx                # 게임 허브 (다음 경기 요약, 바로가기)
│   │       ├── squad/page.tsx          # 선수단 조회
│   │       ├── opponent/page.tsx       # 상대 분석
│   │       ├── tactics/page.tsx        # 전술 설정
│   │       ├── competition/page.tsx    # 대회 진행 현황 (대진표)
│   │       └── match/[matchId]/
│   │           ├── page.tsx            # 경기 진행 (텍스트 중계 + 선택지)
│   │           └── result/page.tsx     # 경기 결과
│   ├── components/
│   │   ├── ui/                         # Button, Card, Badge 등 범용 컴포넌트
│   │   ├── team/                       # TeamCard, TeamGradeBadge
│   │   ├── player/                     # PlayerRow, StatBar, TraitChip
│   │   ├── tactics/                    # FormationPicker, TacticOptionGroup
│   │   ├── match/                      # EventFeed, ChoiceModal, ScoreBoard, MinuteBar
│   │   └── competition/                # BracketView
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts               # fetch 래퍼 (baseURL, 에러 처리)
│   │   │   ├── teams.ts
│   │   │   ├── saveGames.ts
│   │   │   ├── matches.ts
│   │   │   └── competitions.ts
│   │   └── utils.ts
│   └── types/
│       ├── team.ts  player.ts  tactic.ts  match.ts  competition.ts
├── public/
├── tailwind.config.ts
├── next.config.ts
└── package.json
```

포인트:
- 게임 진행 화면은 전부 `game/[saveGameId]/` 아래에 두어 저장 게임 컨텍스트를 URL로 유지.
- 서버 상태는 MVP에선 `fetch` + 페이지 단위 로딩으로 충분. 경기 진행 화면만 클라이언트 컴포넌트로 상태 관리(선택지 응답 → 이어서 시뮬레이션 호출). 필요해지면 TanStack Query 도입.

## Backend (Spring Boot, 도메인 기준 패키지)

```
backend/
├── build.gradle
├── settings.gradle
└── src/main/java/com/fmlite/
    ├── FmLiteApplication.java
    ├── common/
    │   ├── config/           # CorsConfig, JpaConfig, (확장) SecurityConfig
    │   ├── exception/        # GlobalExceptionHandler, ErrorCode, BusinessException
    │   └── response/         # ApiResponse<T> 공통 래퍼
    ├── user/
    │   ├── UserController.java  UserService.java  UserRepository.java  User.java
    ├── team/
    │   ├── TeamController.java  TeamService.java  TeamRepository.java
    │   ├── Team.java  TeamGrade.java
    │   └── dto/              # TeamSummaryResponse, TeamDetailResponse
    ├── player/
    │   ├── PlayerRepository.java  Player.java  Position.java  PlayerTrait.java
    │   └── dto/              # PlayerResponse
    ├── savegame/
    │   ├── SaveGameController.java  SaveGameService.java  SaveGameRepository.java
    │   ├── SaveGame.java  SaveGameStatus.java
    │   └── dto/
    ├── competition/
    │   ├── CompetitionController.java  CompetitionService.java
    │   ├── TournamentProgressService.java     # 라운드 완료 판정, 다음 라운드 대진 생성, 우승 처리
    │   ├── CompetitionRepository.java  Competition.java  Round.java
    │   └── dto/              # BracketResponse
    ├── match/
    │   ├── MatchController.java  MatchService.java  MatchRepository.java
    │   ├── Match.java  MatchStatus.java
    │   ├── tactic/
    │   │   ├── TacticController.java  TacticService.java  TacticRepository.java
    │   │   ├── Tactic.java  Formation.java  Mentality.java  Pressing.java
    │   │   ├── LineHeight.java  AttackStyle.java
    │   ├── event/
    │   │   ├── MatchEvent.java  MatchEventType.java  MatchEventRepository.java
    │   ├── result/
    │   │   ├── MatchResult.java  MatchResultRepository.java
    │   ├── analysis/
    │   │   └── OpponentAnalysisService.java   # 규칙 기반 상대 분석 텍스트 생성
    │   └── simulation/                        # ★ 경기 시뮬레이션 엔진 (05 문서 참조)
    │       ├── MatchSimulationService.java    # 오케스트레이터
    │       ├── TeamPowerCalculator.java
    │       ├── TacticEvaluationService.java
    │       ├── OpponentAiService.java
    │       ├── MatchEventGenerator.java
    │       ├── MatchChoiceService.java
    │       ├── MatchResultCalculator.java
    │       └── model/        # SimulationState, TeamState, TickOutcome, ChoiceOption
    └── seed/
        └── DataSeeder.java   # 앱 기동 시 teams/players 비어있으면 seed 삽입 (또는 SQL 스크립트)
```

포인트:
- 레이어 우선이 아니라 **도메인 우선 패키지** (team, player, match...). MVP 규모에서 탐색이 쉬움.
- 시뮬레이션 엔진은 `match.simulation` 아래 격리. 엔진 내부는 DB를 모르게(순수 자바 객체 입출력) 만들어 단위 테스트 용이하게 함.
- JPA(Hibernate) 사용, 스키마는 Flyway 마이그레이션으로 관리 (`src/main/resources/db/migration/V1__init.sql`, `V2__seed.sql`).

## docs 구조

```
docs/
├── 00-overview.md            # 개요, 게임 루프, MVP 범위
├── 01-architecture.md        # 아키텍처, 디렉터리 구조
├── 02-domain-design.md       # 도메인 모델
├── 03-database-erd.md        # ERD + DDL + seed 전략
├── 04-api-spec.md            # REST API 명세
├── 05-simulation-design.md   # 시뮬레이션 서비스 구조 + 알고리즘
├── 06-frontend-screens.md    # 화면 구성
├── 07-roadmap.md             # 구현 우선순위 + 확장 기능
└── decisions/                # (선택) ADR — 중요한 설계 결정 기록
    └── 001-rule-based-simulation.md
```

## 배포

| 대상 | 방법 |
|---|---|
| Frontend | Vercel — `frontend/` 루트 지정, `NEXT_PUBLIC_API_BASE_URL` 환경변수로 백엔드 주소 주입 |
| Backend | Dockerfile 1개로 Railway / Fly.io / Render 중 택1 (무료·저가 티어로 MVP 충분) |
| DB | Supabase 프로젝트 생성 → 연결 문자열을 백엔드 환경변수(`SPRING_DATASOURCE_URL` 등)로 주입 |
