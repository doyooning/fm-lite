-- 하프타임 전술 변경을 위한 WAITING_HALFTIME 상태 추가
alter table matches drop constraint matches_status_check;
alter table matches add constraint matches_status_check
    check (status in ('SCHEDULED', 'IN_PROGRESS', 'WAITING_CHOICE', 'WAITING_HALFTIME', 'FINISHED'));

-- 사용자 지정 선발 라인업 (player_id 배열, 11명). null 이면 포메이션 기준 베스트 XI 자동 선발
alter table tactics add column lineup jsonb;
