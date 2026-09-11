---
name: dev
description: FM Lite 로컬 개발 스택(Docker PostgreSQL + Spring Boot + Next.js)을 기동하고 상태를 점검한다. "서버 띄워줘", "로컬 실행", "개발환경 켜줘", "왜 안 떠?" 같은 요청이나 E2E 테스트 전 준비에 사용.
---

# /dev — 로컬 개발 스택 기동 & 점검

구성: DB는 Docker PostgreSQL(호스트 포트 **5433**), 백엔드 8080, 프론트 3000.
스키마·시드는 백엔드 기동 시 Flyway가 자동 적용한다.

## 1. 현재 상태부터 확인 (이미 떠 있는 걸 또 띄우지 않는다)

```bash
docker ps -a --filter name=fmlite-pg --format "{{.Names}} {{.Status}}"
curl -s -o /dev/null -w "backend %{http_code}\n" http://localhost:8080/api/v1/teams
curl -s -o /dev/null -w "frontend %{http_code}\n" http://localhost:3000
```

## 2. 기동

**DB** — 컨테이너가 없으면 생성, 있으면 start:
```bash
docker start fmlite-pg || docker run -d --name fmlite-pg \
  -e POSTGRES_USER=fmlite -e POSTGRES_PASSWORD=fmlite -e POSTGRES_DB=fmlite \
  -p 5433:5432 postgres:16-alpine
```

**백엔드** — 장시간 실행이므로 반드시 백그라운드로:
```bash
cd backend && ./gradlew bootRun          # PowerShell이면 .\gradlew.bat bootRun
```
Bash 도구의 `run_in_background`로 띄우고, 로그에서 `Started FmLiteApplication` 과
`Successfully applied ... migration` 을 확인한 뒤 다음 단계로 간다.

**프론트** — 역시 백그라운드:
```bash
cd frontend && npm run dev               # http://localhost:3000
```

## 3. 스모크 확인

```bash
curl -s http://localhost:8080/api/v1/teams | head -c 300     # 팀 8개
node tools/e2e-test.mjs                                      # 풀 플로우 (가입→우승까지)
```

## 4. 자주 나는 문제

| 증상 | 원인 / 조치 |
|---|---|
| 백엔드 기동 실패 — `Connection refused :5433` | 컨테이너 미기동. `docker start fmlite-pg` |
| `FlywayValidateException` (체크섬 불일치) | 적용된 `V*.sql`을 수정함 → 원복하고 새 마이그레이션으로 분리 (`/migration`) |
| Flyway 실패 후 재기동도 실패 | `flyway_schema_history`의 `success=false` 행 삭제 후 재시도 |
| 프론트에서 CORS 오류 | `WebConfig` / `FRONTEND_ORIGIN` 확인 |
| 401이 계속 뜸 | 토큰 만료. 브라우저 localStorage의 `fmlite.token` 삭제 후 재로그인 |
| `application.yml` 없음 | `.gitignore`가 `*.yml`을 제외함. `application.yml.example` 을 복사해 생성 |

## 5. 정리

사용자가 종료를 요청하면 백그라운드 작업을 정리하고 `docker stop fmlite-pg` 한다.
요청 없이 임의로 내리지 않는다.
