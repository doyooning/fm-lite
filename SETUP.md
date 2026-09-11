# FM Lite — 실행 & 배포 셋업

## 1. 로컬 개발 (현재 검증 완료된 구성)

```bash
# 1) DB — Docker PostgreSQL (포트 5433)
docker start fmlite-pg   # 최초 생성 시:
# docker run -d --name fmlite-pg -e POSTGRES_USER=fmlite -e POSTGRES_PASSWORD=fmlite \
#   -e POSTGRES_DB=fmlite -p 5433:5432 postgres:16-alpine

# 2) 백엔드 (스키마+시드는 Flyway가 기동 시 자동 적용)
cd backend && ./gradlew bootRun          # http://localhost:8080

# 3) 프론트엔드
cd frontend && npm run dev               # http://localhost:3000
```

- 백엔드 테스트(몬테카를로 밸런스 포함): `cd backend && ./gradlew test`
- API 전체 플로우 스모크 테스트: `node tools/e2e-test.mjs`

## 2. Supabase 연결

Supabase는 관리형 PostgreSQL로만 사용한다. **연결에는 DB 비밀번호가 필요**하며,
현재 `env/environment.md`의 secret API key(`sb_secret_...`)로는 SQL 실행(DDL)과
JDBC 접속이 모두 불가능하다 (PostgREST 데이터 접근만 가능).

### 방법 A (권장) — DB 비밀번호로 Flyway 자동 적용

Supabase 대시보드 → Settings → Database 에서 비밀번호 확인/재설정 후,
백엔드를 아래 환경변수로 실행하면 기동 시 스키마+시드가 자동 생성된다.

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres.klkjooqskbmrskbjowrb
SPRING_DATASOURCE_PASSWORD=<db-password>
DB_POOL_SIZE=5
```

(연결 문자열은 대시보드 → Connect → Session pooler 값 사용. 무료 티어 커넥션 제한 때문에
pool size는 5 이하 권장.)

### 방법 B — SQL Editor에 수동 적용

[db/supabase-init.sql](db/supabase-init.sql) (스키마 V1 + 시드 V2 통합본)을
Supabase SQL Editor에 붙여넣어 실행한다.

> 주의: 방법 B로 만든 뒤 나중에 백엔드를 Flyway와 함께 연결하려면 기동 전에
> `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`, `SPRING_FLYWAY_BASELINE_VERSION=6` 을 줘서
> 기존 테이블을 V6까지 적용된 상태로 인식시켜야 한다.
> (`db/supabase-init.sql` 은 V1~V6 통합본이다. 마이그레이션을 추가하면 이 번호도 함께 올릴 것.)

- RLS: 현재 해제 상태(사용자 확인). 백엔드가 유일한 DB 클라이언트이므로 MVP에서는 문제없고,
  추후 적용 시에도 JDBC(postgres 롤) 경유라 정책 영향 없음.

## 2-1. 리전 이전 (예: 시드니 → 서울)

Supabase는 프로젝트 생성 후 리전을 바꿀 수 없으므로 **새 프로젝트를 만들어 옮긴다.**
DB 왕복 지연이 응답시간에 그대로 곱해지기 때문에(시드니 기준 쿼리 1회 ≈ 0.46초) 체감 효과가 크다.

1. Supabase에서 **서울(ap-northeast-2)** 리전으로 새 프로젝트 생성
2. 스키마 적용 — 둘 중 하나
   - **Flyway (권장)**: 빈 DB 그대로 두고, 아래 5번의 환경변수만 새 프로젝트 값으로 바꿔 백엔드를 기동하면
     V1~V6이 자동 적용된다. 별도 SQL 실행이 필요 없다.
   - **SQL Editor**: `db/supabase-init.sql`(V1~V6 통합본)을 붙여넣어 실행.
     이후 Flyway와 연결하려면 2절의 baseline 설정(V6)이 필요하다.
3. 데이터 이관이 필요하면 (계정·저장 게임을 유지하려는 경우에만):
   ```bash
   # 스키마는 위에서 이미 만들었으므로 데이터만 옮긴다
   pg_dump --data-only --no-owner --disable-triggers      "postgresql://<구 프로젝트 접속문자열>" > data.sql
   psql "postgresql://<신 프로젝트 접속문자열>" -f data.sql
   ```
   MVP 단계에서 테스트 계정뿐이라면 생략하고 새로 시작하는 편이 깔끔하다.
4. 시드 중복 주의 — 2번에서 스키마와 함께 시드(팀/선수)가 들어간다. 3번으로 데이터를 이관한다면
   `teams`/`players`는 제외하거나, 반대로 시드 없이 스키마만 만든 뒤 전체를 이관할 것.
5. 접속정보 교체 — `env/.env` 의 `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` 를
   새 프로젝트의 **Session pooler** 값으로 변경 후 백엔드 재기동
6. 확인
   ```bash
   curl -s -o /dev/null -w "%{time_total}s
" http://localhost:8080/api/v1/matches/1/events
   ```
   시드니에서 0.4~0.5초대였던 값이 서울 리전에서는 크게 줄어야 한다.

## 3. 배포

| 대상 | 방법 |
|---|---|
| Frontend | Vercel — Root Directory를 `frontend/`로 지정, 환경변수 `NEXT_PUBLIC_API_BASE_URL=https://<백엔드 주소>/api/v1` |
| Backend | Railway/Fly.io/Render 등에 Dockerfile 배포(2절의 Supabase 환경변수 + `FRONTEND_ORIGIN=https://<vercel 도메인>` 주입) |
| DB | Supabase (위 2절) |

## 4. 시드 데이터 재생성 (선택)

```bash
node tools/generate-seed.mjs > backend/src/main/resources/db/migration/V2__seed.sql
```

시드 값이 코드에 고정되어 있어 같은 결과가 재생성된다. 팀/선수 구성을 바꾸려면
`tools/generate-seed.mjs`의 TEAMS/SQUAD_PLAN/GRADE_BASE를 수정.
