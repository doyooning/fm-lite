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
    traits       text[] not null default '{}',
    created_at   timestamptz not null default now(),
    unique (team_id, back_number)
);
create index idx_players_team on players (team_id);

-- ===== 사용자 / 게임 상태 =====
create table users (
    id         uuid primary key,
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
    unique (save_game_id)
);

create table matches (
    id               bigint generated always as identity primary key,
    competition_id   bigint not null references competitions (id),
    round            varchar(10) not null check (round in ('QF', 'SF', 'FINAL')),
    match_no         int    not null,
    home_team_id     bigint not null references teams (id),
    away_team_id     bigint not null references teams (id),
    is_user_match    boolean not null default false,
    status           varchar(20) not null default 'SCHEDULED'
                     check (status in ('SCHEDULED', 'IN_PROGRESS', 'WAITING_CHOICE', 'FINISHED')),
    simulation_state jsonb,
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
    event_type         varchar(20) not null,
    team_id            bigint references teams (id),
    player_id          bigint references players (id),
    description        text   not null,
    requires_choice    boolean not null default false,
    choice_options     jsonb,
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
