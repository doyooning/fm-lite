# 03. Supabase PostgreSQL — ERD & 테이블 설계

## ERD

```
users ──1:N── save_games ──1:1── competitions ──1:N── matches ──1:2── tactics
                 │                                       │
                 └──▶ teams (선택 팀 FK)                  ├──1:N── match_events
                                                         └──1:1── match_results
teams ──1:N── players

matches.home_team_id / away_team_id ──▶ teams
```

- 마스터: `teams`, `players` (seed 후 불변)
- 게임 상태: `save_games`, `competitions`, `matches`, `tactics`, `match_events`, `match_results`
- enum은 PostgreSQL enum 타입 대신 **varchar + CHECK 제약**을 사용 (마이그레이션·JPA 매핑이 단순함)

## DDL (Flyway `V1__init.sql`)

```sql
-- ===== 마스터 데이터 =====
create table teams (
    id          bigint generated always as identity primary key,
    name        varchar(50)  not null unique,
    short_name  varchar(10)  not null,
    grade       varchar(20)  not null
                check (grade in ('STRONG', 'UPPER_MID', 'MID', 'WEAK')),
    description varchar(200) not null default '',
    created_at  timestamptz  not null default now()
);

create table players (
    id           bigint generated always as identity primary key,
    team_id      bigint      not null references teams (id),
    name         varchar(50) not null,
    position     varchar(2)  not null check (position in ('GK', 'DF', 'MF', 'FW')),
    back_number  int         not null,
    attack       int not null check (attack between 1 and 99),
    defense      int not null check (defense between 1 and 99),
    passing      int not null check (passing between 1 and 99),
    speed        int not null check (speed between 1 and 99),
    stamina      int not null check (stamina between 1 and 99),
    mentality    int not null check (mentality between 1 and 99),
    finishing    int not null check (finishing between 1 and 99),
    goalkeeping  int not null check (goalkeeping between 1 and 99),
    traits       text[] not null default '{}',   -- PlayerTrait enum 코드 배열 (0~2개)
    created_at   timestamptz not null default now(),
    unique (team_id, back_number)
);
create index idx_players_team on players (team_id);

-- ===== 사용자 / 게임 상태 =====
create table users (
    id         uuid primary key default gen_random_uuid(),
    nickname   varchar(30) not null default '감독',
    created_at timestamptz not null default now()
);

create table save_games (
    id         bigint generated always as identity primary key,
    user_id    uuid   not null references users (id),
    team_id    bigint not null references teams (id),
    status     varchar(20) not null default 'IN_PROGRESS'
               check (status in ('IN_PROGRESS', 'CHAMPION', 'ELIMINATED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_save_games_user on save_games (user_id);

create table competitions (
    id             bigint generated always as identity primary key,
    save_game_id   bigint not null references save_games (id),
    name           varchar(50) not null default 'FM 챔피언스 컵',
    type           varchar(20) not null default 'SINGLE_ELIM_8',
    current_round  varchar(10) not null default 'QF'
                   check (current_round in ('QF', 'SF', 'FINAL', 'FINISHED')),
    winner_team_id bigint references teams (id),
    created_at     timestamptz not null default now(),
    unique (save_game_id)                      -- MVP: 저장 게임당 대회 1개
);

create table matches (
    id               bigint generated always as identity primary key,
    competition_id   bigint not null references competitions (id),
    round            varchar(10) not null check (round in ('QF', 'SF', 'FINAL')),
    match_no         int    not null,           -- 라운드 내 순번 (대진표 위치)
    home_team_id     bigint not null references teams (id),
    away_team_id     bigint not null references teams (id),
    is_user_match    boolean not null default false,
    status           varchar(20) not null default 'SCHEDULED'
                     check (status in ('SCHEDULED', 'IN_PROGRESS', 'WAITING_CHOICE', 'FINISHED')),
    simulation_state jsonb,                     -- 진행 중 스냅샷 (선택지 대기 시 재개용)
    created_at       timestamptz not null default now(),
    unique (competition_id, round, match_no)
);
create index idx_matches_competition on matches (competition_id);

create table tactics (
    id           bigint generated always as identity primary key,
    match_id     bigint not null references matches (id),
    team_id      bigint not null references teams (id),
    formation    varchar(10) not null check (formation in ('4-3-3', '4-2-3-1', '3-5-2')),
    mentality    varchar(10) not null check (mentality in ('ATTACKING', 'BALANCED', 'DEFENSIVE')),
    pressing     varchar(10) not null check (pressing in ('LOW', 'NORMAL', 'HIGH')),
    line_height  varchar(10) not null check (line_height in ('LOW', 'NORMAL', 'HIGH')),
    attack_style varchar(12) not null check (attack_style in ('CENTER', 'WIDE', 'COUNTER', 'POSSESSION')),
    updated_at   timestamptz not null default now(),
    unique (match_id, team_id)
);

create table match_events (
    id                 bigint generated always as identity primary key,
    match_id           bigint not null references matches (id),
    seq                int    not null,
    minute             int    not null,
    event_type         varchar(20) not null,   -- KICK_OFF/CHANCE/GOAL/SAVE/MISS/HALF_TIME/
                                               -- TACTIC_CHANGE/CHOICE/FULL_TIME/PENALTY_SHOOTOUT
    team_id            bigint references teams (id),
    player_id          bigint references players (id),
    description        text   not null,
    requires_choice    boolean not null default false,
    choice_options     jsonb,                  -- [{"id":"PUSH_FORWARD","label":"...","description":"..."}]
    selected_choice_id varchar(30),
    created_at         timestamptz not null default now(),
    unique (match_id, seq)
);
create index idx_match_events_match on match_events (match_id);

create table match_results (
    id                  bigint generated always as identity primary key,
    match_id            bigint not null unique references matches (id),
    home_score          int    not null,
    away_score          int    not null,
    penalty_home_score  int,
    penalty_away_score  int,
    winner_team_id      bigint not null references teams (id),
    stats               jsonb  not null default '{}',
    created_at          timestamptz not null default now()
);
```

## Seed 데이터 전략 (Flyway `V2__seed.sql` 또는 `DataSeeder`)

### 팀 8개 (등급 분포: 강 2 / 중상 2 / 중 2 / 약 2)

| 팀 | 등급 | 콘셉트 |
|---|---|---|
| 레드 스톰 FC | STRONG | 화력 중심 최강팀 |
| 블루 나이츠 | STRONG | 단단한 수비의 강호 |
| 골든 이글스 | UPPER_MID | 빠른 역습 |
| 실버 울브스 | UPPER_MID | 미드필드 장악 |
| 그린 파이터스 | MID | 균형형 |
| 오렌지 웨이브 | MID | 측면 공격 |
| 퍼플 가디언스 | WEAK | 수비 일변도 |
| 화이트 폭스 | WEAK | 도전자 |

### 능력치 생성 규칙 (자동 생성 스크립트 기준)

등급별 기준치 `base` + 포지션 보정 + 랜덤 편차:

| 등급 | base |
|---|---|
| STRONG | 78 |
| UPPER_MID | 72 |
| MID | 66 |
| WEAK | 60 |

```
능력치 = clamp(base + 포지션보정 + uniform(-6, +6), 1, 99)

포지션 보정 (주능력 +8, 부능력 +3, 반대능력 -12):
  GK: goalkeeping +12, mentality +3, attack/finishing -20
  DF: defense +8, mentality +3, finishing -10
  MF: passing +8, stamina +3
  FW: finishing +8, attack +8, speed +3, defense -10
```

- 팀당 18명: GK 2, DF 6, MF 6, FW 4 (요구사항 고정)
- 특성: 선수당 0~2개를 포지션에 맞게 랜덤 부여 (PK_SAVER는 GK만, AERIAL_THREAT는 DF/FW 위주 등). 팀당 최소 3명은 특성 보유하도록 보정 — 상대 분석 화면의 읽을거리가 됨.
- 생성 방식: 로컬에서 한 번 스크립트로 생성 → 결과를 `V2__seed.sql`에 INSERT 문으로 고정. **매 배포마다 랜덤이 아니라 seed 고정**이 밸런싱·디버깅에 유리.

### 대진 생성 규칙

새 게임 시작 시: 8팀을 셔플하되 STRONG 2팀은 서로 반대 사이드에 배치(시드 배정) → QF 4경기 생성. 사용자 팀이 포함된 경기는 `is_user_match = true`.
