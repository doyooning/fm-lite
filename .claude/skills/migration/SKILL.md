---
name: migration
description: Flyway DB 마이그레이션을 규칙에 맞게 추가한다. 다음 V번호 확인, DDL 작성, 엔티티/ERD 문서/db/supabase-init.sql 동기화까지 챙긴다. "테이블 추가", "컬럼 추가", "마이그레이션 만들어줘", "DB 스키마 변경" 요청에 사용.
---

# /migration — Flyway 마이그레이션 추가

## 절대 규칙

- **이미 적용된 `V*.sql` 파일은 절대 수정하지 않는다.** 체크섬이 깨져 기동이 실패한다.
  (아직 어디에도 적용되지 않은 직전 마이그레이션이라면 사용자에게 확인 후에만 수정)
- 변경은 항상 **새 파일**: `backend/src/main/resources/db/migration/V<다음번호>__<snake_case_설명>.sql`
- 시드 데이터(팀/선수)는 `tools/generate-seed.mjs`가 생성원. `V2__seed.sql`을 손으로 고치지 않는다.
  시드를 바꾸려면 생성기를 수정하고 재생성하되, 이미 배포된 DB에는 별도 마이그레이션이 필요하다.

## 1. 현황 확인

```bash
ls backend/src/main/resources/db/migration
```
가장 큰 V번호 + 1 이 이번 파일 번호다. `docs/03-database-erd.md`에서 기존 테이블 구조를 확인한다.

## 2. DDL 작성 기준

- PostgreSQL 16 문법. 기존 마이그레이션의 스타일(네이밍, 제약조건 명명)을 따른다.
- 기존 테이블에 NOT NULL 컬럼을 추가할 때는 **`DEFAULT` 를 함께 주거나 3단계(추가 → 백필 → NOT NULL)** 로 쓴다. 기존 행이 있으면 그냥 NOT NULL은 실패한다.
- 외래키는 `REFERENCES <table>(id)` + 필요 시 `ON DELETE CASCADE` (게임 삭제 기능이 있으므로 save_game 하위 테이블은 특히 확인).
- 조회 경로에 맞는 인덱스를 함께 만든다 (예: `save_game_id`, `user_id` 기준 조회).
- jsonb 컬럼을 쓸 때는 무엇이 들어가는지 주석으로 남긴다.

## 3. 함께 반영할 것 (빠뜨리기 쉬움)

| 대상 | 할 일 |
|---|---|
| JPA 엔티티 (`backend/src/main/java/com/fmlite/<도메인>/*.java`) | 필드/애너테이션 추가 — **구현은 Codex 담당이면 명세에만 적는다** |
| `db/supabase-init.sql` | Supabase SQL Editor용 통합본. 새 DDL을 말미에 반영 (수동 적용 경로 유지) |
| `docs/03-database-erd.md` | ERD·DDL 문서 갱신 — **직접 갱신** |
| `SETUP.md` | 베이스라인 버전 안내(`SPRING_FLYWAY_BASELINE_VERSION`)가 낡지 않았는지 확인 |
| `CLAUDE.md` | `(현재 V<N>까지)` 표기를 이번 번호로 갱신 — **직접 갱신** |

## 4. 검증

```bash
docker start fmlite-pg
cd backend && ./gradlew bootRun     # 기동 시 Flyway가 자동 적용 — 로그에서 Migrating to version <N> 확인
```
적용 결과 확인:
```bash
docker exec fmlite-pg psql -U fmlite -d fmlite -c "select version, description, success from flyway_schema_history order by installed_rank desc limit 5;"
```

실패 시 `flyway_schema_history`에 실패 행이 남아 재기동도 막힌다 → 해당 행을 지우고 SQL을 고친 뒤 재시도한다.

## 5. 역할 경계

- 마이그레이션 SQL 자체와 문서 갱신은 이 스킬에서 **직접** 작성한다 (스키마는 설계 산출물).
- 엔티티/리포지토리/서비스 코드 변경은 명세서에 적어 Codex에게 넘긴다.
