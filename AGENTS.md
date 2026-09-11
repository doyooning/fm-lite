# FM Lite — 에이전트 작업 규약

축구 감독 시뮬레이션 게임. 모노레포: `backend/`(Spring Boot) + `frontend/`(Next.js) + `db/` + `tools/` + `docs/`.
프로젝트 개요는 `README.md`, 실행 방법은 `SETUP.md`, 설계는 `docs/` 참조.

## 역할 분담

| 에이전트 | 담당 |
|---|---|
| Claude Code | 기능 설계, 구현 명세서(`docs/specs/`) 작성, DB 마이그레이션 SQL, 문서 갱신, 구현 결과 검증 |
| Codex | 명세서/지시서에 따른 실제 코드 구현 (backend/frontend), 자체 테스트 통과 |

Codex는 작업 전 해당 `docs/specs/NNN-*.handoff.md` 와 이 문서를 읽는다.
지시서에 없는 파일은 수정하지 않는다. 명세와 다르게 구현해야 할 이유가 생기면 **먼저 보고**한다.

## 기술 스택

- 백엔드: Spring Boot 3.5.6, Java 17, Gradle, JPA, Flyway, Spring Security + JWT(jjwt), 패키지 `com.fmlite`
- 프론트: Next.js 16 (App Router), React 19, TypeScript, Tailwind v4, 소스는 `frontend/src/`
- DB: PostgreSQL 16 (로컬 Docker 포트 5433 / 운영 Supabase)

> `frontend/AGENTS.md` 경고 참고: 이 Next.js는 최신 버전이라 학습 데이터와 API가 다를 수 있다.
> 확실하지 않으면 `node_modules/next/dist/docs/` 를 읽고 쓸 것.

## 코드 규약

**백엔드**
- 도메인별 패키지 구성 (`match/`, `team/`, `player/`, `savegame/`, `competition/`, `auth/`, `profile/`).
  컨트롤러/서비스/리포지토리/엔티티는 도메인 패키지 안에, 요청·응답 객체는 `dto/` 하위에.
- 응답은 항상 `ApiResponse` 래퍼: `{ "success": true, "data": ... }` / 오류 `{ "success": false, "error": { "code", "message" } }`.
- 예외는 `BusinessException(code, message)` → `GlobalExceptionHandler`가 변환. 컨트롤러에서 try-catch 하지 않는다.
- 인증이 필요한 API는 `@CurrentUserId UUID userId` 파라미터를 받고, 자원 소유권을 반드시 검증한다
  (`MatchAccess`, `SaveGameService`의 기존 패턴을 따를 것).
- Lombok 사용. 엔티티는 기존 파일들의 애너테이션 스타일을 그대로 따른다.
- **경기 판정 로직은 전부 규칙 기반.** 난수는 시뮬레이션 엔진 내부에서만 쓰고, LLM 호출을 판정에 넣지 않는다.
- DB 변경은 항상 새 Flyway 파일(`V<N>__<name>.sql`). **기존 마이그레이션 수정 금지.**

**프론트엔드**
- API 호출은 `src/lib/api/` 를 통해서만. 컴포넌트에서 `fetch` 직접 호출 금지.
- 응답 타입은 `src/types/api.ts` 에 백엔드 DTO와 1:1로 정의. nullable을 임의로 생략하지 않는다.
- 인증 토큰은 `localStorage`의 `fmlite.token` (`src/lib/auth.ts` 경유). 401 처리는 `lib/api/client.ts`가 담당.
- UI 텍스트는 한국어. 공용 컴포넌트는 `src/components/ui.tsx` 재사용.

**공통**
- 커밋 메시지는 한국어, `feat:` / `fix:` / `refactor:` / `docs:` / `chore:` 접두사.
- `env/`, `*.yml`, `.env*` 는 커밋하지 않는다 (`.gitignore` 참조). 설정 템플릿은 `*.example` 로만.
- 요청 범위를 넘는 리팩터링·포맷팅·의존성 추가를 하지 않는다.

## 검증 명령

```bash
cd backend && ./gradlew test        # 단위 + 몬테카를로 밸런스 테스트
cd frontend && npx tsc --noEmit     # 타입 체크
cd frontend && npm run lint
node tools/e2e-test.mjs             # 백엔드+DB 기동 상태에서 풀 플로우
```

시뮬레이션(`match/simulation/`)을 건드렸다면 `MatchEngineBalanceTest` 결과를 확인한다:
평균 득점 2.5~3.0, 강팀 대 약팀 승률 70~80% 범위를 벗어나면 실패로 본다.
