-- 저장 게임별 감독 이름 (새 게임 생성 시 사용자가 설정)
alter table save_games add column manager_name varchar(30) not null default '감독';
