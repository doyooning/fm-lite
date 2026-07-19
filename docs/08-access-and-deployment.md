# 08. 접속 구조 & 배포 전환 가이드

프론트는 Vercel, 백엔드는 로컬로 배포한 현재 구조의 동작 조건과,
이후 "어디서나 접속 가능"하게 전환하는 방법을 정리한다.

## 로그인 & 이메일 인증의 도달성 (중요)

로그인 필수 + 이메일 인증(가입 시 링크 발송)이 도입됐다. 인증 링크는 백엔드 환경변수
`FRONTEND_BASE_URL`을 기준으로 `<FRONTEND_BASE_URL>/verify?token=...` 형태로 만들어지고,
그 `/verify` 페이지가 다시 백엔드(`NEXT_PUBLIC_API_BASE_URL`)의 `POST /auth/verify`를 호출한다.

- 현재는 백엔드가 로컬(localhost:8080)이므로 **인증 완료도 백엔드를 켠 PC의 브라우저에서만** 가능
  (앱 전체와 동일 제약). Gmail SMTP 미설정 시에도 백엔드 로그에 인증 링크가 출력되어 개발 테스트는 가능.
- 배포/실사용 전환 시 조정할 환경변수 (코드 수정 불필요):
  - 백엔드 `env/.env`: `FRONTEND_BASE_URL`(인증 링크 도메인), `FRONTEND_ORIGIN`(CORS),
    `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM`(Gmail SMTP), `JWT_SECRET`
  - 프론트(Vercel): `NEXT_PUBLIC_API_BASE_URL`(백엔드 공개 주소)
- **백엔드 배포는 추후 Cloudtype 예정.** Cloudtype에 배포해 고정 HTTPS 주소가 생기면
  위 `NEXT_PUBLIC_API_BASE_URL`을 그 주소로 바꾸고 재배포하면, 어느 기기에서든 가입·인증·플레이가
  가능해진다. (`FRONTEND_BASE_URL`은 프론트 도메인 = Vercel 주소 그대로 유지.)

## 현재 구조 (as-is)

```
[사용자 브라우저] ──HTTPS──▶ [Vercel: Next.js]  https://fm-lite-gamma.vercel.app
       │  (API 호출은 브라우저에서 직접 발생 — 클라이언트 컴포넌트)
       └──HTTP──▶ [로컬 백엔드]  http://localhost:8080  ──JDBC──▶ [Supabase PostgreSQL]
```

- API 호출 주소는 빌드 시 주입된 `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`.
- 호출이 **브라우저에서** 나가므로, `localhost:8080`은 **그 브라우저가 실행 중인 PC**의 백엔드를 가리킨다.

### 동작 조건 (중요)

| 조건 | 동작 |
|---|---|
| 백엔드를 켠 그 PC의 브라우저에서 접속 | ✅ 동작 (Chrome은 `http://localhost` mixed-content 예외 허용) |
| 같은 PC라도 백엔드가 꺼져 있음 | ❌ API 호출 실패 |
| 다른 기기 / 다른 네트워크에서 접속 | ❌ 그 기기의 `localhost`엔 백엔드가 없음 |
| 일부 비-Chromium 브라우저 | ⚠ HTTPS→HTTP(localhost) 차단 가능 |

> 즉 현재 배포는 **개발자 본인 시연용**이다. 외부 공유하려면 아래 전환이 필요하다.

---

## 전환 방법

핵심은 하나다: **백엔드를 공개 HTTPS 주소로 노출**하고, 그 주소를 프론트(`NEXT_PUBLIC_API_BASE_URL`)와
백엔드 CORS(`FRONTEND_ORIGIN`) 양쪽에 반영한 뒤 프론트를 재배포한다.

### 방법 A — 터널 (백엔드는 계속 로컬, 가장 빠름)

로컬 백엔드를 임시 공개 HTTPS URL로 뚫는다. 시연·테스트에 적합(무료 티어는 URL이 매번 바뀔 수 있음).

**cloudflared 예시**
```bash
# 1) 백엔드는 평소대로 로컬 실행 (localhost:8080)
# 2) 터널 실행 → https://<랜덤>.trycloudflare.com 발급
cloudflared tunnel --url http://localhost:8080
```
**ngrok 예시**
```bash
ngrok http 8080     # → https://<랜덤>.ngrok-free.app 발급
```

발급된 주소(예: `https://abcd.trycloudflare.com`)로 3단계를 수행 → [공통: 주소 반영](#공통-새-백엔드-주소-반영) 참조.
이때 `NEXT_PUBLIC_API_BASE_URL`은 `https://abcd.trycloudflare.com/api/v1`.

- 장점: 백엔드 이전 없이 즉시 외부 접속 가능, HTTPS라 mixed-content 문제 없음.
- 단점: 로컬 PC가 켜져 있어야 하고, 무료 URL은 재시작 시 변경 → 매번 프론트 env 갱신 필요.

### 방법 B — 백엔드 클라우드 배포 (상시 운영, 권장 최종형)

백엔드를 Railway / Fly.io / Render 등에 배포해 고정 HTTPS 주소를 얻는다.

1. 백엔드 컨테이너화 (아래 Dockerfile 참고) 후 호스팅 플랫폼에 배포.
2. 플랫폼 환경변수로 아래를 주입 (`env/.env` 대신):
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres
   SPRING_DATASOURCE_USERNAME=postgres.klkjooqskbmrskbjowrb
   SPRING_DATASOURCE_PASSWORD=<db-password>
   DB_POOL_SIZE=5
   FRONTEND_ORIGIN=https://fm-lite-gamma.vercel.app
   PORT=8080            # 플랫폼이 지정하는 포트를 따르면 그 값 사용
   ```
   > `application.yml`의 `config.import`는 `optional:`이라 파일이 없으면 자동으로 환경변수를 사용한다.
3. 배포된 주소(예: `https://fm-lite-api.up.railway.app`)로 [공통: 주소 반영](#공통-새-백엔드-주소-반영) 수행.

**최소 Dockerfile (backend/Dockerfile)**
```dockerfile
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar -x test
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

- 장점: 로컬 PC 불필요, 고정 주소, 상시 운영.
- 단점: 무료 티어 콜드 스타트 가능. Supabase 무료 커넥션 제한 때문에 `DB_POOL_SIZE`는 낮게 유지.

---

## 공통: 새 백엔드 주소 반영

백엔드 공개 주소를 `<API_BASE>`(예: `https://abcd.trycloudflare.com`)라 하면:

**1) 백엔드 CORS 허용 (FRONTEND_ORIGIN)** — Vercel 도메인이 이미 들어있으니 보통 그대로 두면 됨.
   프리뷰 도메인 등 추가가 필요하면 `env/.env`(또는 배포 플랫폼 환경변수)에서 콤마로 나열:
   ```
   FRONTEND_ORIGIN=http://localhost:3000,https://fm-lite-gamma.vercel.app
   ```
   변경 시 백엔드 재기동.

**2) 프론트 API 주소 갱신 후 재배포** (프로젝트 루트가 아니라 `frontend/`에서 실행):
   ```bash
   cd frontend
   # 토큰은 env/environment.md 의 Vercel Token 사용
   npx vercel deploy --prod --yes --token <VERCEL_TOKEN> \
     -b NEXT_PUBLIC_API_BASE_URL=<API_BASE>/api/v1
   ```
   > `NEXT_PUBLIC_*`은 **빌드 시점에 값이 박히므로**, 주소가 바뀌면 반드시 재배포해야 한다.
   > 프로덕션 별칭(`https://fm-lite-gamma.vercel.app`)은 재배포해도 그대로 유지된다.

**3) 검증**: 배포 사이트를 (이번엔 아무 기기에서나) 열어 팀 목록 로딩 + 새 게임 생성이 되는지 확인.
   실패 시 브라우저 콘솔에서 CORS/네트워크 오류를 확인한다.

---

## 참고: GitHub 연동 자동 배포 (선택)

지금은 CLI로 로컬 파일을 올리는 방식이다. Vercel 대시보드에서 `doyooning/fm-lite` 저장소를
Import(Root Directory=`frontend`)하고 `NEXT_PUBLIC_API_BASE_URL`을 프로젝트 환경변수로 등록해 두면,
이후 `main` 푸시마다 자동 배포된다. 이 경우 `-b` 플래그 없이도 빌드 시 값이 주입된다.
