-- 이메일/비밀번호 계정 인증 도입
-- users 확장 (컬럼은 nullable — 소셜 로그인 등 forward-compat, 가입 필수 검증은 앱 레벨)
alter table users add column email          varchar(255);
alter table users add column password_hash  varchar(100);
alter table users add column email_verified boolean not null default false;
alter table users add column updated_at     timestamptz not null default now();

-- 이메일 중복 불허 (대소문자 무시)
create unique index uq_users_email on users (lower(email));

-- 이메일 인증 토큰 (불투명 랜덤 토큰, 만료/사용여부 관리)
create table email_verification_tokens (
    id         bigint generated always as identity primary key,
    user_id    uuid not null references users (id),
    token      varchar(120) not null unique,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);
create index idx_evt_user on email_verification_tokens (user_id);
