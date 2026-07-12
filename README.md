# FM Lite — 축구 감독 시뮬레이션 (MVP)

가상 팀 8개로 구성된 싱글 엘리미네이션 토너먼트에서, 사용자가 한 팀의 감독이 되어
전술을 설정하고 텍스트 기반 경기 시뮬레이션을 통해 우승에 도전하는 "FM 라이트 버전" 게임.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind CSS |
| Backend | Spring Boot 3.x, Java 21, Gradle |
| Database | Supabase PostgreSQL (관리형 PostgreSQL로만 사용) |
| Auth | MVP: 익명 세션 → 확장 시 Spring Security + JWT |
| 배포 | Frontend: Vercel / Backend: Railway·Fly.io 등 컨테이너 호스팅 |

## 핵심 원칙

- **경기 판정은 규칙 기반(Rule Engine), AI(LLM)는 설명·리포트 등 부가 기능에만 추후 사용**
- 상대 팀 전술 변화도 상태 기반 규칙 AI로 구현
- MVP 완성 우선. 선수 성장/이적/시즌 모드 등은 확장 단계로 미룸

## 실행 방법

[SETUP.md](SETUP.md) 참조 — 로컬 개발(Docker PostgreSQL), Supabase 연결, 배포 절차.

```
fm-lite/
├── backend/    # Spring Boot API + 규칙 기반 시뮬레이션 엔진 (Flyway 마이그레이션 포함)
├── frontend/   # Next.js 화면 9종
├── db/         # Supabase SQL Editor용 통합 초기화 SQL
├── tools/      # 시드 생성기, E2E 스모크 테스트
└── docs/       # 설계 문서
```

## 설계 문서

| 문서 | 내용 |
|---|---|
| [docs/00-overview.md](docs/00-overview.md) | 프로젝트 개요, 게임 루프, MVP 범위 |
| [docs/01-architecture.md](docs/01-architecture.md) | 시스템 아키텍처, 디렉터리 구조 (FE/BE/docs) |
| [docs/02-domain-design.md](docs/02-domain-design.md) | 백엔드 도메인 모델 설계 |
| [docs/03-database-erd.md](docs/03-database-erd.md) | ERD, 테이블 DDL, seed 데이터 전략 |
| [docs/04-api-spec.md](docs/04-api-spec.md) | REST API 명세 초안 |
| [docs/05-simulation-design.md](docs/05-simulation-design.md) | 경기 시뮬레이션 서비스 구조 + 최소 알고리즘 |
| [docs/06-frontend-screens.md](docs/06-frontend-screens.md) | Next.js 화면 구성 및 라우팅 |
| [docs/07-roadmap.md](docs/07-roadmap.md) | 구현 우선순위(8단계) + 확장 기능 목록 |
| [docs/08-access-and-deployment.md](docs/08-access-and-deployment.md) | 접속 구조(프론트=Vercel/백엔드=로컬) 동작 조건 + 외부 접속 전환 가이드 |
