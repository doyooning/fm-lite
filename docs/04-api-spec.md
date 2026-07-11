# 04. REST API 명세 초안 (MVP)

- Base URL: `/api/v1`
- 응답 포맷: `{ "success": true, "data": {...} }` / 오류: `{ "success": false, "error": { "code", "message" } }`
- 인증: MVP는 `X-User-Id` 헤더(익명 UUID)만 전달. 확장 시 `Authorization: Bearer <JWT>`로 교체 (엔드포인트 변경 없음).

## 1. 사용자

| Method | Path | 설명 |
|---|---|---|
| POST | `/users` | 익명 유저 생성 (프론트 최초 접속 시). 응답의 id를 localStorage 보관 |

## 2. 팀 / 선수 (마스터 조회)

| Method | Path | 설명 |
|---|---|---|
| GET | `/teams` | 팀 목록 (팀 선택 화면) |
| GET | `/teams/{teamId}` | 팀 상세 |
| GET | `/teams/{teamId}/players` | 선수단 조회 |

`GET /teams` 응답 예:
```json
{ "success": true, "data": [
  { "id": 1, "name": "레드 스톰 FC", "shortName": "RSF", "grade": "STRONG",
    "description": "화력 중심 최강팀", "avgRating": 78 }
]}
```

`GET /teams/1/players` 응답 예:
```json
{ "success": true, "data": [
  { "id": 11, "name": "김강찬", "position": "FW", "backNumber": 9,
    "stats": { "attack": 84, "defense": 45, "passing": 70, "speed": 82,
               "stamina": 75, "mentality": 77, "finishing": 86, "goalkeeping": 10 },
    "traits": [ { "code": "BIG_GAME_PLAYER", "name": "빅게임에 강함" } ] }
]}
```

## 3. 저장 게임 (새 게임 / 이어하기)

| Method | Path | 설명 |
|---|---|---|
| POST | `/save-games` | 새 게임 시작. body: `{ "teamId": 1 }`. SaveGame + Competition + 8강 대진 생성 |
| GET | `/save-games/{id}` | 저장 게임 상태 (선택 팀, 대회 진행, 상태) |
| GET | `/save-games/{id}/next-match` | 사용자 팀의 다음 경기 (없으면 대회 종료 정보) |
| GET | `/users/{userId}/save-games` | 이어하기 목록 |

`GET /save-games/1/next-match` 응답 예:
```json
{ "success": true, "data": {
  "matchId": 3, "round": "QF", "status": "SCHEDULED",
  "homeTeam": { "id": 1, "name": "레드 스톰 FC" },
  "awayTeam": { "id": 5, "name": "그린 파이터스" },
  "isUserHome": true, "tacticSubmitted": false
}}
```

## 4. 대회

| Method | Path | 설명 |
|---|---|---|
| GET | `/competitions/{id}` | 대회 상태 + 대진표 (라운드별 경기·스코어·진출팀) |

응답 예:
```json
{ "success": true, "data": {
  "id": 1, "name": "FM 챔피언스 컵", "currentRound": "SF", "winnerTeamId": null,
  "rounds": [
    { "round": "QF", "matches": [
      { "matchId": 1, "homeTeam": {...}, "awayTeam": {...},
        "status": "FINISHED", "score": "2-1", "winnerTeamId": 1, "isUserMatch": true }
    ]},
    { "round": "SF", "matches": [ ... ] },
    { "round": "FINAL", "matches": [] }
  ]
}}
```

## 5. 상대 분석

| Method | Path | 설명 |
|---|---|---|
| GET | `/matches/{matchId}/opponent-analysis` | 상대 팀 규칙 기반 분석 |

응답 예 (모두 규칙/템플릿으로 생성 — 확장 시 LLM 문장으로 교체 가능한 구조):
```json
{ "success": true, "data": {
  "team": { "id": 5, "name": "그린 파이터스", "grade": "MID" },
  "powerByArea": { "attack": 66, "midfield": 68, "defense": 64, "goalkeeping": 65 },
  "strengths": ["미드필드 패스 전개가 안정적입니다."],
  "weaknesses": ["수비진 스피드가 느려 뒷공간에 취약합니다."],
  "keyPlayers": [ { "id": 55, "name": "박정우", "position": "MF", "reason": "PASS_MASTER 특성 보유" } ],
  "expectedTactic": { "formation": "4-2-3-1", "mentality": "BALANCED", "attackStyle": "POSSESSION" }
}}
```

## 6. 전술

| Method | Path | 설명 |
|---|---|---|
| GET | `/matches/{matchId}/tactics/me` | 내 전술 조회 (없으면 팀 기본값) |
| PUT | `/matches/{matchId}/tactics/me` | 경기 전 전술 저장 |

PUT body:
```json
{ "formation": "4-3-3", "mentality": "ATTACKING", "pressing": "HIGH",
  "lineHeight": "NORMAL", "attackStyle": "WIDE" }
```

## 7. 경기 시뮬레이션 (핵심 플로우)

| Method | Path | 설명 |
|---|---|---|
| POST | `/matches/{matchId}/start` | 시뮬레이션 시작. 첫 **선택지 이벤트 또는 경기 종료**까지의 이벤트 목록 반환 |
| POST | `/matches/{matchId}/choices` | 선택지 응답. body: `{ "eventId": 45, "choiceId": "PUSH_FORWARD" }`. 다음 선택지/종료까지 이어서 시뮬레이션 |
| GET | `/matches/{matchId}/events` | 이벤트 전체 로그 (새로고침 복구용, `?afterSeq=` 지원) |
| GET | `/matches/{matchId}/result` | 경기 결과 (FINISHED 상태에서만) |

**진행 프로토콜** — `start`/`choices` 응답 공통 형태:
```json
{ "success": true, "data": {
  "matchStatus": "WAITING_CHOICE",
  "score": { "home": 1, "away": 0 },
  "minute": 58,
  "events": [
    { "seq": 12, "minute": 43, "eventType": "GOAL", "teamId": 1,
      "playerId": 11, "description": "김강찬의 침투! 골키퍼를 넘기는 침착한 마무리!" },
    { "seq": 13, "minute": 58, "eventType": "CHOICE", "requiresChoice": true, "eventId": 45,
      "description": "상대가 라인을 올리며 압박해 옵니다. 어떻게 대응할까요?",
      "choiceOptions": [
        { "id": "COUNTER_FOCUS", "label": "역습 노림", "description": "뒷공간 침투 위주로 전환 (고위험 고효율)" },
        { "id": "KEEP_CALM", "label": "점유 유지", "description": "안정적으로 볼을 돌린다" },
        { "id": "PARK_BUS", "label": "수비 강화", "description": "리드 지키기 (찬스는 줄어듦)" }
      ] }
  ]
}}
```
- `matchStatus: "FINISHED"`이면 프론트는 결과 화면으로 이동.
- 프론트는 events 배열을 받아 **타이핑/딜레이 연출로 순차 표시** — 서버는 한 번에 계산해 반환하므로 구조가 단순함 (SSE/WebSocket 불필요).
- 멱등성: `WAITING_CHOICE`가 아닌 상태에서 `choices` 호출 시 409, 이미 시작된 경기에 `start` 재호출 시 기존 이벤트 로그 반환.

## 8. 상태 코드 규약

| 코드 | 상황 |
|---|---|
| 400 | 유효하지 않은 전술 값, 잘못된 choiceId |
| 404 | 리소스 없음 |
| 409 | 상태 충돌 (전술 미설정 상태에서 start, FINISHED 경기에 choices 등) |
