---
name: handoff
description: 구현 명세서(docs/specs/NNN-*.md)를 Codex가 바로 실행할 수 있는 작업 지시 프롬프트로 변환해 docs/specs/NNN-*.handoff.md 에 저장하고 출력한다. "Codex에 넘겨줘", "핸드오프", "구현 지시서 만들어줘" 또는 /verify 이후 재작업 지시가 필요할 때 사용.
---

# /handoff — Codex 구현 지시서 생성

인자: 명세서 경로 (`docs/specs/NNN-slug.md`). 생략 시 `docs/specs/`에서 가장 최근 명세서를 쓰되,
사용자에게 대상 파일명을 확인받는다.

## 1. 준비

- 명세서를 끝까지 읽는다.
- 명세서가 참조하는 실제 파일들이 지금도 존재하는지 확인한다 (경로가 틀린 지시서는 Codex를 헤매게 만든다).
- 이전 핸드오프의 재작업이면 `/verify` 결과(실패 항목)를 지시서 맨 앞 "이번에 고칠 것"에 넣는다.

## 2. 지시서 형식

`docs/specs/NNN-slug.handoff.md` 로 저장. Codex는 대화 맥락이 없으므로 **자기완결적**이어야 한다.

````markdown
# [기능명] 구현 지시

## 컨텍스트
- 저장소 루트: C:\claude_projects\fm-lite (Windows)
- 백엔드: Spring Boot 3.5 / Java 17 / Gradle / Flyway, 패키지 com.fmlite
- 프론트: Next.js 16 App Router / TypeScript / Tailwind v4, src/ 구조
- 규약은 루트 `AGENTS.md` 를 먼저 읽을 것. 설계 근거는 `docs/specs/NNN-slug.md`.

## 작업 범위
(명세서의 목표 그대로. 여기 없는 파일은 건드리지 않는다.)

## 하지 않을 것
(명세서의 비목표 + 리팩터링/포맷팅/의존성 추가 금지)

## 변경 사항
### 1. DB — backend/src/main/resources/db/migration/V<N>__<name>.sql (신규)
(전체 DDL 그대로)

### 2. backend/src/main/java/com/fmlite/... (신규/수정)
파일별로: 무엇을 / 시그니처 / 주의점

### 3. frontend/src/... (신규/수정)
파일별로: 타입 정의, API 함수, 화면 변경

## 완료 기준
- [ ] (검증 가능한 항목들)

## 자체 검증 (구현 후 Codex가 직접 실행)
```bash
cd backend && ./gradlew test
cd frontend && npx tsc --noEmit && npm run lint
```
기대: 전부 통과. 실패하면 고친 뒤 다시 실행.

## 보고
변경한 파일 목록과, 명세와 다르게 구현한 부분이 있으면 그 이유를 함께 남길 것.
````

## 3. 지시서를 쓸 때 지킬 것

- **코드 전체를 다 써주지 않는다.** DDL·API 계약·시그니처·제약은 정확히 주고, 구현 본문은 Codex에게 맡긴다.
- 애매한 표현 금지. "적절히 처리" 대신 "`BusinessException("NOT_FOUND", ...)` 을 던진다".
- 기존 코드 패턴을 따라야 하는 곳은 참고 파일을 콕 집어준다 (예: "`MatchController`의 `@CurrentUserId` 사용 패턴을 따를 것").
- 한 지시서는 한 번에 리뷰 가능한 크기로. 너무 크면 명세서를 단계로 쪼개 핸드오프를 2개 만든다.

## 4. 마무리

지시서 경로를 알려주고, Codex에 붙여넣을 한 줄 실행 프롬프트를 함께 출력한다:

```
docs/specs/NNN-slug.handoff.md 를 읽고 그대로 구현해줘. 루트 AGENTS.md의 규약을 지킬 것.
```
