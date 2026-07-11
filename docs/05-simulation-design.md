# 05. 경기 시뮬레이션 설계 (규칙 기반)

## 설계 원칙

1. **엔진은 순수 자바** — DB/HTTP를 모른다. 입력(팀·선수·전술·이전 상태) → 출력(이벤트 목록 + 새 상태). 단위 테스트로 밸런싱 가능.
2. **틱(tick) 기반** — 90분을 5분 단위 18틱으로 나눠 진행. 틱마다 "찬스 발생 여부 → 찬스 해결(골/선방/실축)"만 판정. 분 단위 정밀 시뮬레이션은 하지 않음.
3. **일시정지 가능** — 선택지 이벤트에서 멈추고 상태를 `matches.simulation_state`(jsonb)에 저장, 사용자의 선택을 받아 재개.
4. **시드 고정 RNG** — 경기 시작 시 랜덤 시드를 상태에 저장. 재현 가능한 버그 리포트/테스트.

## 서비스 구조

```
MatchController
   │
   ▼
MatchSimulationService (오케스트레이터)
   │  ├─ 시작: 라인업 자동 선발, AI 전술 생성, 상태 초기화
   │  ├─ 진행: 다음 선택지/종료 지점까지 틱 루프 실행
   │  └─ 종료: 결과 저장, TournamentProgressService 호출
   │
   ├──▶ TeamPowerCalculator        # 선발 XI → 지역별 전력 (attack/midfield/defense/gk)
   │                               # 특성·컨디션 보정 포함
   ├──▶ TacticEvaluationService    # 내 전술 vs 상대 전술 → 상성 보정치 계산
   ├──▶ OpponentAiService          # 틱마다 상태(스코어/시간/체력) 보고 AI 전술 조정
   ├──▶ MatchEventGenerator        # 틱 결과 → 이벤트 객체 + 중계 텍스트(템플릿)
   ├──▶ MatchChoiceService         # 선택지 생성 시점 판단 + 선택 효과 적용
   └──▶ MatchResultCalculator      # 종료 처리: 통계 집계, 동점 시 승부차기, MatchResult 생성

TournamentProgressService (competition 패키지)
   └─ 라운드 완료 확인 → AI vs AI 경기 즉시 시뮬레이션 → 다음 라운드 대진 생성 → 우승 처리
```

### SimulationState (jsonb 직렬화 대상)

```java
class SimulationState {
    long rngSeed; int rngCallCount;      // 재개 시 RNG 복원
    int currentTick;                     // 0~17 (5분 단위)
    int homeScore, awayScore;
    TeamState home, away;
    int nextChoiceTick;                  // 다음 선택지 예정 틱
    List<String> activeChoiceEffects;    // 사용자 선택 효과 (지속 틱 수 포함)
}
class TeamState {
    long teamId;
    Tactic currentTactic;                // AI는 경기 중 변경될 수 있음
    Map<Long, Double> playerCondition;   // 경기 시작 시 0.85~1.10 랜덤 생성
    double teamStamina;                  // 1.0에서 시작, 틱마다 감소
    int momentum;                        // -3 ~ +3 (골/연속 찬스로 변동)
}
```

## 최소 알고리즘 (MVP)

### 0단계 — 경기 준비 (start 호출 시 1회)

1. **선발 XI 자동 선발**: 포메이션별 포지션 슬롯(예: 4-3-3 = GK1, DF4, MF3, FW3)에
   해당 포지션 선수 중 종합치 높은 순으로 배치.
2. **컨디션 생성**: 선수마다 `uniform(0.85, 1.10)` — 경기마다 새로 생성 (이월 없음).
3. **AI 팀 초기 전술**: 팀 등급/콘셉트별 기본 전술 테이블에서 선택
   (예: WEAK 팀은 DEFENSIVE + LOW 라인, 역습 콘셉트 팀은 COUNTER).
4. **지역별 전력 계산** (`TeamPowerCalculator`):

```
attackPower  = avg(FW: attack·finishing·speed, 공격형 MF 일부) × 컨디션 × 특성 보정
midfieldPower= avg(MF: passing·stamina·mentality) × 컨디션 × 특성 보정
defensePower = avg(DF: defense·speed·mentality) × 컨디션 × 특성 보정
gkPower      = GK: goalkeeping × 컨디션
```

5. **전술 상성 보정** (`TacticEvaluationService`) — 작은 상성표 하나로 충분:

| 조건 | 효과 |
|---|---|
| 내 pressing=HIGH vs 상대 attackStyle=POSSESSION | 상대 midfieldPower -8% |
| 내 attackStyle=COUNTER vs 상대 mentality=ATTACKING | 내 찬스 질 +15% |
| 내 attackStyle=WIDE vs 상대 formation=3-5-2 | 내 찬스 확률 +10% |
| 상대 lineHeight=HIGH & 내 팀에 RUN_IN_BEHIND 보유 | 그 선수 찬스 선정 가중치 2배 |
| mentality=ATTACKING | 내 찬스 확률 +15%, 상대 찬스 확률 +10% (양날) |
| mentality=DEFENSIVE | 내 찬스 확률 -15%, 상대 찬스 확률 -10% |
| pressing=HIGH | 상대 찬스 확률 -5%, 내 체력 소모 +50% |

### 1단계 — 틱 루프 (5분 × 18틱)

틱마다 순서대로:

```
1. OpponentAiService.adjust(state)        # 아래 규칙 테이블
2. 체력 감소: teamStamina -= 0.010 × pressingFactor
   (teamStamina < 0.8이면 모든 전력에 ×teamStamina 적용,
    LOW_STAMINA 특성 선수는 개인 컨디션 추가 하락)
3. 지배력 계산:
   dominance = (myMidfield × 전술보정 × momentum보정)
             / (myMidfield + oppMidfield)
4. 찬스 판정: chanceProb = 0.28 × dominance × mentality보정   # 팀별 독립 판정
   → roll < chanceProb 이면 찬스 발생 (양 팀 모두 발생 시 dominance 높은 쪽 우선)
5. 찬스 해결:
   a. 공격수 선정: FW/공격 MF 중 (attack+finishing) 가중 랜덤 (특성 가중치 반영)
   b. goalProb = 0.30 × (attackerScore / (attackerScore + defenseGkScore)) × 2
      attackerScore = (finishing×0.6 + attack×0.4) × 컨디션 × 찬스질보정
      defenseGkScore = (oppDefense×0.5 + oppGk×0.5)
   c. roll → GOAL / SAVE / MISS  (대략 틱당 골 확률 ~4%, 경기당 합계 2~3골 수준)
6. 이벤트 기록: MatchEventGenerator가 템플릿으로 중계 텍스트 생성
   골/선방 시 momentum ±1 조정
7. 선택지 틱 확인: currentTick ∈ {6(30분), 11(55분), 15(75분)} 이면
   MatchChoiceService가 상황 맞는 선택지 생성 → 상태 저장 후 루프 중단(WAITING_CHOICE)
```

숫자(0.28, 0.30 등)는 시작값. `MatchSimulationServiceTest`에서 1,000경기 몬테카를로 돌려
평균 득점 2.5~3.0, 강팀 vs 약팀 승률 70~80%가 나오도록 상수만 튜닝한다.

### 2단계 — 경기 중 선택지 (`MatchChoiceService`)

상황(스코어, 시간, momentum)에 맞는 선택지 세트를 고르고, 선택은 **이후 N틱 동안의 보정치**로 적용:

| 상황 | 선택지 예 | 효과 (남은 경기 또는 4틱 지속) |
|---|---|---|
| 지고 있음 (후반) | 총공격 / 유지 / 침착하게 | 총공격: 내 찬스 +30%, 상대 찬스 +25% |
| 이기고 있음 (후반) | 수비 강화 / 유지 / 역습 노림 | 수비 강화: 상대 goalProb -15%, 내 찬스 -30% |
| 동점 & momentum 높음 | 몰아치기 / 유지 | 몰아치기: 2틱간 찬스 +40%, 이후 체력 -0.05 |
| 상대 전술 변경 감지 | 맞대응 / 무시 | 맞대응: 상성 보정 재계산 |

### 3단계 — 상대 AI 규칙 (`OpponentAiService`, 틱마다 평가)

| 조건 | 조정 |
|---|---|
| 60분 이후 & 지는 중 | mentality 한 단계 공격적으로, lineHeight ↑ |
| 70분 이후 & 이기는 중 | mentality 한 단계 수비적으로, pressing ↓ |
| teamStamina < 0.75 | pressing 한 단계 ↓ |
| 최근 3틱 내 측면 찬스 2회 이상 허용 | (사용자 attackStyle=WIDE 대응) 수비 보정 +5%, 단 midfield -3% |
| 사용자 lineHeight=HIGH | 30% 확률로 attackStyle=COUNTER 전환 (뒷공간 침투) |

조정 발생 시 `TACTIC_CHANGE` 이벤트로 기록 → 사용자에게 "상대가 라인을 내립니다" 같은 중계 텍스트 노출 (맞대응 선택지 트리거).

### 4단계 — 종료 (`MatchResultCalculator`)

1. 18틱 완료 → `FULL_TIME` 이벤트.
2. 동점이면 간이 승부차기: 5라운드, 키커 성공률 = `0.75 + (mentality-70)×0.003 - GK PK보정(PK_SAVER면 +0.15 선방)`.
3. 통계 집계(점유율≈평균 dominance, 슛/유효슛=찬스/온타깃 수, 베스트 플레이어=공격포인트/선방 가중) → `match_results` 저장.
4. `TournamentProgressService.onMatchFinished()`:
   - 같은 라운드 AI vs AI 경기들을 **선택지 없이 같은 엔진으로 즉시 시뮬레이션**.
   - 라운드 완료 → 승자들로 다음 라운드 Match 생성.
   - 결승 종료 → `competitions.winner_team_id` 기록, SaveGame.status = CHAMPION 또는 ELIMINATED.
   - 사용자 패배 시: 남은 대회를 자동 시뮬레이션으로 마무리 후 ELIMINATED.

## 중계 텍스트 (MVP)

`MatchEventGenerator`가 이벤트 타입별 템플릿 풀에서 랜덤 선택 + 변수 치환:

```
GOAL 템플릿 예:
- "{minute}' GOAL! {player}의 {style} 마무리! {team}이 앞서갑니다!"
- "{minute}' 골망을 가릅니다! {player}!"
SAVE: "{minute}' {gk}의 슈퍼세이브! {player}의 슛이 막힙니다."
```

타입별 5~8개면 충분히 다채로움. 확장 단계에서 이 계층만 LLM 호출로 교체하면 됨 (판정 로직 무변경).
