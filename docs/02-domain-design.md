# 02. 백엔드 도메인 설계

## 도메인 지도

```
[마스터 데이터 — 읽기 전용]          [게임 상태 — 저장 게임마다 생성]
┌──────────┐                       ┌──────────┐
│  Team    │◀──── 선택 ──────────── │ SaveGame │──── 1:1 ───▶ Competition
│  (8팀)   │                       └──────────┘                  │ 1:N
└────┬─────┘                            │                        ▼
     │ 1:18                             ▼                     ┌───────┐
┌──────────┐                        User (MVP: 익명)          │ Match │
│  Player  │                                                  └───┬───┘
└──────────┘                                                      │
                                              ┌───────────┬───────┴────────┐
                                              ▼ 1:2       ▼ 1:N           ▼ 1:1
                                           Tactic     MatchEvent      MatchResult
                                        (홈/원정 각 1)
```

**설계 원칙**: Team/Player는 마스터 데이터(seed로 고정, 게임 중 불변). 게임 진행 상태는
SaveGame을 루트로 하는 별도 애그리거트에만 기록한다. 덕분에 새 게임을 몇 번이든 시작해도
마스터 데이터는 복사할 필요가 없다. (선수 성장 기능을 넣는 시점에 SaveGamePlayer 스냅샷
테이블을 도입하면 됨 — 확장 문서 참조.)

## 엔티티 상세

### User
MVP에서는 로그인 없이, 프론트가 처음 접속할 때 익명 유저를 생성해 localStorage에 id 보관.

| 필드 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK |
| nickname | String | 선택 입력, 기본 "감독" |
| createdAt | Instant | |

### Team (마스터)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| name / shortName | String | 팀명 / 약칭 |
| grade | Enum `TeamGrade` | STRONG, UPPER_MID, MID, WEAK |
| description | String | 팀 소개 한 줄 (팀 선택 화면용) |

### Player (마스터)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| teamId | Long | FK → Team |
| name | String | |
| position | Enum `Position` | GK, DF, MF, FW |
| backNumber | int | |
| attack, defense, passing, speed, stamina, mentality, finishing, goalkeeping | int (1~99) | 능력치 8종 |
| traits | List&lt;Enum `PlayerTrait`&gt; | 0~2개 |

`PlayerTrait` (MVP 9종):

| 코드 | 이름 | 시뮬레이션 효과 (예) |
|---|---|---|
| BIG_GAME_PLAYER | 빅게임에 강함 | 4강/결승에서 능력치 +5% |
| LOW_STAMINA | 체력 약함 | 후반 체력 감소 2배 |
| WEAK_UNDER_PRESSURE | 압박에 약함 | 상대 pressing=HIGH일 때 passing -10% |
| LONG_SHOT | 중거리 슛 선호 | 중거리 찬스 발생 확률 증가 |
| POOR_CONCENTRATION | 수비 집중력 낮음 | 75분 이후 수비 기여 -10% |
| RUN_IN_BEHIND | 침투 선호 | 상대 lineHeight=HIGH일 때 찬스 확률 증가 |
| AERIAL_THREAT | 공중볼 강함 | wide 공격 찬스에서 결정력 +10% |
| PASS_MASTER | 패스 마스터 | 팀 midfieldPower 계산 시 가중 +5 |
| PK_SAVER | PK 선방 강함 | 승부차기 선방 확률 +15%p (GK 전용) |

### SaveGame (게임 상태 루트)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| userId | UUID | FK → User |
| teamId | Long | 사용자가 선택한 팀 |
| status | Enum `SaveGameStatus` | IN_PROGRESS, CHAMPION, ELIMINATED |
| createdAt / updatedAt | Instant | |

### Competition

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| saveGameId | Long | FK → SaveGame (MVP 1:1) |
| name | String | 예: "FM 챔피언스 컵" |
| type | Enum | SINGLE_ELIM_8 (MVP 고정) |
| currentRound | Enum `Round` | QF, SF, FINAL, FINISHED |
| winnerTeamId | Long? | 우승 팀 (종료 시) |

### Match

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| competitionId | Long | FK |
| round | Enum `Round` | QF / SF / FINAL |
| matchNo | int | 라운드 내 순번 (대진표 위치) |
| homeTeamId / awayTeamId | Long | FK → Team |
| isUserMatch | boolean | 사용자 팀 포함 여부 |
| status | Enum `MatchStatus` | SCHEDULED → IN_PROGRESS → WAITING_CHOICE → FINISHED |
| simulationState | JSON | 진행 중 상태 스냅샷 (분, 스코어, 체력, 모멘텀, RNG seed). 선택지 대기 중 재개용 |

**상태 전이**
```
SCHEDULED ──(전술 설정 후 시작)──▶ IN_PROGRESS ──(선택지 이벤트)──▶ WAITING_CHOICE
                                      ▲                                │
                                      └────────(선택 제출)─────────────┘
                                      │
                                      └──(90분 종료/승부차기)──▶ FINISHED
```

### Tactic
경기별·팀별 1개. AI 팀 전술은 시뮬레이션 시작 시 규칙으로 자동 생성.

| 필드 | 타입 | 값 |
|---|---|---|
| id | Long | PK |
| matchId / teamId | Long | 복합 unique |
| formation | Enum | F_4_3_3, F_4_2_3_1, F_3_5_2 |
| mentality | Enum | ATTACKING, BALANCED, DEFENSIVE |
| pressing | Enum | LOW, NORMAL, HIGH |
| lineHeight | Enum | LOW, NORMAL, HIGH |
| attackStyle | Enum | CENTER, WIDE, COUNTER, POSSESSION |

### MatchEvent
경기 로그이자 선택지 저장소. `seq` 순서로 재생하면 중계가 됨.

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| matchId | Long | FK |
| seq | int | 이벤트 순서 |
| minute | int | 경기 시간 |
| eventType | Enum `MatchEventType` | KICK_OFF, CHANCE, GOAL, SAVE, MISS, HALF_TIME, TACTIC_CHANGE(상대 AI), CHOICE, FULL_TIME, PENALTY_SHOOTOUT |
| teamId / playerId | Long? | 관련 팀/선수 |
| description | String | 중계 텍스트 (MVP: 템플릿 기반) |
| requiresChoice | boolean | 선택지 이벤트 여부 |
| choiceOptions | JSON? | `[{id, label, description}]` |
| selectedChoiceId | String? | 사용자가 고른 선택지 |

### MatchResult

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| matchId | Long | FK, unique |
| homeScore / awayScore | int | 정규시간 스코어 |
| penaltyHomeScore / penaltyAwayScore | int? | 승부차기 (동점 시) |
| winnerTeamId | Long | 토너먼트라 항상 존재 |
| stats | JSON | `{possession, shots, shotsOnTarget, bestPlayerId}` 등 요약 통계 |

## 애그리거트 & 트랜잭션 경계

- `SaveGame` 생성 트랜잭션: SaveGame + Competition + QF Match 4개 생성.
- `Match` 시뮬레이션 트랜잭션: 이벤트 배치 저장 + simulationState 갱신 (+ 종료 시 MatchResult 저장, TournamentProgressService 호출).
- 라운드 완료 처리: 해당 라운드 모든 Match FINISHED → 다음 라운드 Match 생성. 결승 종료 → Competition.winnerTeamId 기록, SaveGame.status = CHAMPION/ELIMINATED.
- 사용자 탈락 시: 남은 대회 경기는 즉시 자동 시뮬레이션으로 마무리(대진표 완성용) 후 SaveGame 종료.
