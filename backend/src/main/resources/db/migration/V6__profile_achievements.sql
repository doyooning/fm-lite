-- 프로필: 누적 우승 횟수 (게임을 삭제해도 유지)
alter table users add column championships int not null default 0;

-- 업적 (계정별 1회 획득)
create table user_achievements (
    id          bigint generated always as identity primary key,
    user_id     uuid not null references users (id),
    code        varchar(40) not null,
    achieved_at timestamptz not null default now(),
    unique (user_id, code)
);
create index idx_user_achievements_user on user_achievements (user_id);
