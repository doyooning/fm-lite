# NNN. [기능명]

- 상태: 설계 완료 / 구현 중 / 검증 완료
- 관련 문서: docs/0X-....md

## 목표

(한두 문단. 무엇을, 왜.)

## 비목표

- (이번 작업에서 하지 않을 것 — 범위 확장 방지)

## DB 변경

파일: `backend/src/main/resources/db/migration/V<N>__<name>.sql` (신규)

```sql
-- 전체 DDL
```

엔티티 매핑:
| 컬럼 | 타입 | 엔티티 필드 | 비고 |
|---|---|---|---|

## API 계약

| Method | Path | 인증 | 설명 |
|---|---|---|---|

요청:
```json
```

응답:
```json
{ "success": true, "data": {} }
```

에러:
| code | 조건 | HTTP |
|---|---|---|

## 백엔드 변경

| 파일 | 신규/수정 | 내용 |
|---|---|---|

## 프론트엔드 변경

| 파일 | 신규/수정 | 내용 |
|---|---|---|

## 시뮬레이션 영향

(없으면 "없음". 있으면 판정 규칙 변경 내용 + 밸런스 재확인 필요 여부)

## 수용 기준

- [ ] (검증 가능한 항목)
- [ ] 

## 검증 방법

```bash
cd backend && ./gradlew test
cd frontend && npx tsc --noEmit && npm run lint
node tools/e2e-test.mjs
```

기대 결과:

## 함께 갱신할 문서

- [ ] docs/0X-....md
