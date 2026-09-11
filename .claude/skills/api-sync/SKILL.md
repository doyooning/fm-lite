---
name: api-sync
description: 백엔드 컨트롤러/DTO, 프론트 types-api.ts + lib/api, docs/04-api-spec.md 세 곳의 API 계약이 일치하는지 대조해 불일치를 보고한다. "API 맞는지 확인", "타입 동기화", "명세랑 코드 다른 것 같아" 또는 API를 바꾼 구현 직후에 사용.
---

# /api-sync — API 계약 3자 동기화 점검

FM Lite의 API 계약은 세 곳에 중복 존재한다. 하나만 바뀌면 조용히 깨진다.

| 소스 | 위치 | 역할 |
|---|---|---|
| **진실의 원천** | `backend/src/main/java/com/fmlite/**/**Controller.java` + `**/dto/*.java` | 실제 동작 |
| 프론트 계약 | `frontend/src/types/api.ts`, `frontend/src/lib/api/index.ts` | 타입/호출 |
| 문서 | `docs/04-api-spec.md` | 사람이 읽는 명세 |

**코드가 기준이다.** 문서가 코드와 다르면 문서를 고친다 (그 반대가 의도라면 사용자에게 확인).

## 1. 수집

```bash
grep -rn "@\(Get\|Post\|Put\|Patch\|Delete\)Mapping\|@RequestMapping" backend/src/main/java --include=*.java
```
- 각 컨트롤러의 경로·메서드·요청 body 타입·응답 타입을 표로 정리한다.
- 대응하는 DTO의 필드명/타입/nullable을 읽는다 (Java `Integer` vs `int`, Optional 여부가 프론트 `| null`과 대응).
- `frontend/src/lib/api/index.ts`의 호출 목록과 `types/api.ts` 인터페이스를 읽는다.
- `docs/04-api-spec.md`의 표와 JSON 예시를 읽는다.

## 2. 대조 항목

- 엔드포인트 존재 여부 (백엔드에 있는데 문서에 없음 / 프론트가 호출하는데 백엔드에 없음)
- HTTP 메서드·경로 파라미터 이름
- 요청 body 필드 (필수/선택, validation 애너테이션)
- 응답 필드명 — 특히 **Java 필드명 → JSON 키** (Lombok/Jackson 기본은 camelCase 그대로)
- nullable: DTO가 null을 줄 수 있는 필드가 프론트 타입에서 non-null로 선언돼 있지 않은가
- enum 값 집합 (`Formation`, `Mentality`, `Pressing`, `AttackStyle`, `LineHeight`, `TeamGrade`, `Position`, `MatchStatus`, `SaveGameStatus`) — 프론트 유니온 타입과 값이 정확히 같은가
- 에러 코드 (`BusinessException`에서 던지는 code 문자열이 프론트 분기와 맞는가)

## 3. 리포트

```
불일치 N건

1. [심각] GET /save-games/{id}/next-match
   백엔드: NextMatchResponse.opponentTactic 은 null 가능 (MatchService.java:88)
   프론트: types/api.ts:120 에서 OpponentTactic (non-null)
   → 프론트 타입을 `| null` 로 수정 필요

2. [문서] POST /matches/{id}/choices
   docs/04-api-spec.md 에 누락
   → 문서에 추가 필요
```

심각도: **[심각]** 런타임 오류 가능 / **[주의]** 타입은 통과하나 의미 불일치 / **[문서]** 문서만 낡음.

## 4. 수정

- **문서(`docs/04-api-spec.md`) 갱신은 직접 한다.**
- 프론트/백엔드 **코드 수정은 Codex 담당**이므로, 수정이 필요하면 지적만 하고 `/handoff` 로 지시서를 만들 것을 권한다.
  (단 `types/api.ts`의 명백한 nullable 누락 등 1~2줄 수정은 직접 고치고 보고한다.)
