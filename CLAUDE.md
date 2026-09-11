# FM Lite — Claude Code 작업 가이드

축구 감독 시뮬레이션 게임(MVP). 8팀 싱글 엘리미네이션 토너먼트, 텍스트 기반 경기 시뮬레이션.

## 이 프로젝트에서의 내 역할

**설계 담당.** 기능 설계 → 구현 명세서 작성 → (Codex가 구현) → 검증.
`backend/src`, `frontend/src` 구현 코드는 Codex 담당이므로 직접 고치지 않는다.
예외: 마이그레이션 SQL, `docs/*`, 오타 수준(1~2줄) 수정.

작업 루프: `/spec` → `/handoff` → (Codex) → `/verify` → 실패 시 `/handoff` 재생성.

| 스킬 | 용도 |
|---|---|
| `/spec` | 기능 설계 + `docs/specs/NNN-slug.md` 명세서 작성 |
| `/handoff` | 명세서 → Codex 지시서 변환 |
| `/verify` | 구현 결과를 명세 대비 검증 (빌드·테스트·E2E·diff 리뷰) |
| `/api-sync` | 백엔드 DTO ↔ 프론트 타입 ↔ API 문서 3자 대조 |
| `/migration` | Flyway 마이그레이션 추가 |
| `/dev` | 로컬 스택 기동/점검 |

## 구조

```
backend/    Spring Boot 3.5.6 / Java 17 / Gradle / JPA / Flyway / Security+JWT (com.fmlite)
frontend/   Next.js 16 App Router / React 19 / TS / Tailwind v4  (소스는 frontend/src)
db/         Supabase SQL Editor용 통합 초기화 SQL
tools/      시드 생성기(generate-seed.mjs), E2E(e2e-test.mjs, auth-e2e.mjs)
docs/       00~08 설계 문서 + specs/ 구현 명세서
```

백엔드 도메인 패키지: `auth` `user` `team` `player` `savegame` `competition` `match`(+`simulation` `tactic` `event` `result` `analysis`) `profile` `security` `common`.

## 규약

코드 규약 전문은 **`AGENTS.md`** (Codex와 공유). 핵심만:

- 응답은 `ApiResponse` 래퍼 고정, 예외는 `BusinessException` → `GlobalExceptionHandler`
- 인증 API는 `@CurrentUserId` + 자원 소유권 검증 필수 (`MatchAccess` 패턴)
- **경기 판정은 규칙 기반.** LLM은 리포트 등 부가 기능에만
- DB 변경은 **항상 새 Flyway 파일**. 적용된 `V*.sql` 수정 금지 (현재 V6까지)
- 시드는 `tools/generate-seed.mjs`가 생성원 — `V2__seed.sql` 손수정 금지
- 문서와 코드가 다르면 **코드가 기준**
- `env/`, `*.yml`, `.env*` 커밋 금지. 커밋 메시지는 한국어 + `feat:`/`fix:`/`refactor:`/`docs:`/`chore:`

## 명령

```bash
docker start fmlite-pg                      # PostgreSQL 16, 호스트 포트 5433
cd backend && ./gradlew bootRun             # :8080  (PowerShell은 .\gradlew.bat)
cd frontend && npm run dev                  # :3000

cd backend && ./gradlew test                # 단위 + 몬테카를로 밸런스
cd frontend && npx tsc --noEmit && npm run lint
node tools/e2e-test.mjs                     # 풀 플로우 (백엔드+DB 기동 필요)
```

시뮬레이션 수정 시 `MatchEngineBalanceTest` 기준: 평균 득점 2.5~3.0, 강팀 대 약팀 승률 70~80%.

## 문서 찾기

`docs/00-overview` 개요 · `01-architecture` 구조 · `02-domain-design` 도메인 · `03-database-erd` ERD ·
`04-api-spec` API · `05-simulation-design` 시뮬레이션 · `06-frontend-screens` 화면 · `07-roadmap` 로드맵 ·
`08-access-and-deployment` 배포. 실행/배포 절차는 `SETUP.md`.
