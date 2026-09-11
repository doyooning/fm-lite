---
name: spec
description: 새 기능/변경 요청을 FM Lite 구조에 맞춰 설계하고, Codex가 그대로 구현할 수 있는 구현 명세서(docs/specs/NNN-slug.md)를 작성한다. "기능 추가", "설계해줘", "스펙 써줘", "이 기능 어떻게 만들지" 같은 요청에 사용. 구현 코드는 작성하지 않는다.
---

# /spec — 기능 설계 & 구현 명세서 작성

FM Lite의 **설계 담당은 Claude Code, 구현 담당은 Codex**다.
이 스킬은 요구사항을 받아 **설계 결정까지만** 내리고, Codex가 추측 없이 구현할 수 있는
명세서를 남긴다. **이 스킬에서는 `backend/src`, `frontend/src`, 마이그레이션 SQL을 직접 수정하지 않는다.**

## 1. 조사 (먼저 읽기)

명세를 쓰기 전에 관련 부분을 실제로 읽는다. 추측 금지.

| 대상 | 확인 사항 |
|---|---|
| `docs/02-domain-design.md`, `docs/03-database-erd.md` | 기존 도메인/테이블과 충돌·중복 여부 |
| `docs/04-api-spec.md` | 기존 엔드포인트 재사용 가능 여부, 응답 래퍼 규칙 |
| `docs/05-simulation-design.md` | 시뮬레이션 판정에 영향이 있는가 (있으면 밸런스 테스트 필수) |
| `docs/06-frontend-screens.md` | 새 화면인가, 기존 화면 확장인가 |
| `backend/src/main/java/com/fmlite/<도메인>` | 실제 클래스 구조 (문서와 코드가 다르면 **코드가 기준**) |
| `frontend/src/types/api.ts`, `frontend/src/lib/api/` | 프론트 계약 현황 |

## 2. 설계 원칙 (프로젝트 고정 규칙)

- **경기 판정은 규칙 기반**. LLM은 설명·리포트 등 부가 기능에만. 판정 로직에 AI를 넣는 설계는 제안하지 않는다.
- 게임 로직은 전부 백엔드. 프론트는 화면 + API 호출만.
- API 응답은 `ApiResponse` 래퍼(`{success, data}` / `{success, error:{code,message}}`) 고정.
- 인증은 `Authorization: Bearer <JWT>` + `@CurrentUserId`. 신규 엔드포인트는 `SecurityConfig`의 공개 경로에 넣지 않는 한 인증 필요.
- 소유권 검증: `saveGame.userId == currentUserId` 확인 없이 자원을 반환하지 않는다 (기존 `MatchAccess` 패턴 참고).
- DB 변경은 **항상 새 Flyway 마이그레이션**. 기존 `V*.sql` 수정 금지 → `/migration` 스킬 참조.
- 시드 데이터(팀/선수)는 `tools/generate-seed.mjs`가 생성원. SQL을 손으로 고치지 않는다.

## 3. 명세서 작성

`docs/specs/` 의 다음 번호를 확인하고 `docs/specs/NNN-<slug>.md` 로 저장한다
(형식은 `docs/specs/TEMPLATE.md` 를 그대로 따른다).

명세서에 **반드시** 들어가야 하는 것:

1. **목표 / 비목표** — 이번 작업에서 하지 않을 것을 명시해 Codex의 범위 확장을 막는다.
2. **DB 변경** — 새 마이그레이션 파일명(다음 V번호), 정확한 DDL, 엔티티 필드 매핑.
3. **API 계약** — 메서드·경로·요청 body·응답 JSON 예시·에러 코드. `docs/04-api-spec.md`에 그대로 옮길 수 있는 수준.
4. **백엔드 변경 파일 목록** — 신규/수정 파일을 경로 단위로. 각 파일에 "무엇을" 하는지 한 줄씩.
5. **프론트 변경 파일 목록** — `types/api.ts` 타입, `lib/api/` 함수, 화면 컴포넌트.
6. **수용 기준(Acceptance Criteria)** — 체크박스로. 각 항목은 검증 가능해야 한다("동작 잘 됨" 금지).
7. **검증 방법** — 실행할 명령(`./gradlew test`, `node tools/e2e-test.mjs` 등)과 기대 결과.
8. **함께 갱신할 문서** — 이 변경으로 낡아지는 `docs/*.md` 목록.

## 4. 마무리

- 명세서 작성 후 관련 `docs/0*.md` 본문도 이번 설계에 맞게 갱신한다 (명세서는 작업 지시, `docs/0*`는 최신 설계 상태).
- 사용자에게 요약 보고: 설계 요점 3~5줄 + 명세서 경로 + "다음은 `/handoff docs/specs/NNN-slug.md`".
- 설계 판단이 갈리는 지점(예: 테이블 추가 vs jsonb 확장)이 있으면 **명세서를 쓰기 전에** 사용자에게 물어본다.
