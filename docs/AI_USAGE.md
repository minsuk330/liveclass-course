# AI 활용 범위

## 사용 도구

- Claude Code
- Codex

## 활용 원칙

본 과제에서 AI는 **"두 번째 의견을 주는 페어 프로그래머"** 로 두었습니다.

- AI 출력은 항상 1차 후보로 받고, 도메인 규칙·코드 구조·테스트 결과로 재검증
- 설계 결정과 트레이드오프는 본인이 직접 판단
- 동일 작업을 다른 관점으로 두 번 묻기 (구현 → 리뷰 분리)

## 작업 흐름

```
요구사항 해석 → 옵션 비교 (AI 보조) → 본인 결정 → spec 작성 → 구현
→ 테스트 작성/실행 → AI 리뷰 요청 → 본인 재검토 → 회귀 테스트 → 제출 문서 갱신
```

spec 초안은 구현 방향을 정리하는 참고 자료로 활용했습니다.
최종 제출 기준 문서는 실제 코드에서 생성한 `docs/openapi.json`, `docs/DB_스키마.sql`, README로 두었고, 설계 과정 문서와 실제 구현이 어긋나는 경우에는 최종 구현 기준으로 README를 갱신했습니다.

## 영역별 활용 + 검증 방식

| 영역 | AI 활용 | 검증/보완 방식 |
|---|---|---|
| 요구사항 해석 | 모호한 항목(취소 가능 기간, 대기열 정책) 옵션 비교표 생성 | 본인이 가정을 직접 선택하고 README "요구사항 해석 및 가정"에 명시 |
| 도메인 설계 | 엔티티/상태 머신 초안 | 컨벤션(`ports.in`, `Default` 접두사, 서브패키지)은 본인이 `CLAUDE.md`에 사전 정의 후 강제 |
| API 명세 | 각 도메인별 spec md 초안, OpenAPI 문서화 방향 검토 | 본인이 path/응답 형식/에러 코드 직접 확정, 최종 명세는 실제 앱의 `/v3/api-docs`에서 생성 |
| 동시성 전략 | 비관적/낙관적/atomic update/분산 락 비교 | 본인이 검증 가능성 + 도메인 단순성 기준으로 비관적 락 채택, 동시성 테스트 직접 작성 |
| 코드 생성 | 보일러플레이트(엔티티, command, DTO) | 매 변경 후 컴파일 + 테스트로 검증, 의도에 맞지 않는 부분 수동 수정 |
| 코드 리뷰 | 검증 순서, URL 컨벤션, 경합 시나리오 점검 요청 | 리뷰 의견을 그대로 수용하지 않고 본인이 trade-off 다시 평가 후 반영 |
| 테스트 | 시나리오 아이디어, assertion 패턴 | 도메인/통합/동시성 3계층으로 직접 분류, Testcontainers 환경 본인이 안정화 |

## AI 검증 사례

**1. 수강 취소에서 비관적 락 위치**

- AI 1차 제안: cancel 메서드 첫 statement에 LiveClass 락
- 본인 검증: `classId`를 알아내려면 `enrollment`를 먼저 조회해야 하므로 첫 statement에 락이 불가능 → AI 제안을 그대로 적용하면 잘못된 코드가 됨
- 본인 결정: `READ_COMMITTED` 격리 수준으로 변경, snapshot 함정 회피
- 결과: 동시성 테스트(`WaitlistConcurrencyTest`)로 두 명 동시 취소 → 정확히 두 명 promote 검증

**2. WaitlistEntry 중복 등록 방지**

- AI 1차 제안: DB unique 제약 제거, 앱 레벨에서만 검증
- 본인 판단: race condition에서는 앱 검증만으로 부족할 수 있음
- 본인 결정: `active_user_id` 보조 컬럼 + `(class_id, active_user_id)` unique → soft delete 시 NULL로 바꿔 재등록 허용. DB 레벨 무결성 + 앱 레벨 친절한 에러 둘 다 확보

**3. WaitlistService 검증 순서**

- AI 1차 생성: 가용성 → 중복 → 활성 신청 순
- 본인 판단: 본인 상태(활성 신청, 중복 대기) 먼저 확인하고 자원 상태(정원 가용성)는 마지막에 두는 게 사용자에게 더 정확한 에러 메시지를 줌
- 본인 결정: 순서 재배치 후 테스트 재실행

**4. 테스트 인프라 hang 디버깅**

- AI 1차 진단: 통합 베이스 + reuse + pool 사이즈로 충분하다고 제안
- 본인 검증: `./gradlew test` 가 14분 stuck, `jstack`으로 직접 thread dump → Hikari pool 고갈 + 컨테이너 reuse 매칭 실패 확인
- 본인 결정: `@Testcontainers`/`@Container` 라이프사이클 우회 → static initializer + `@DynamicPropertySource` 패턴으로 단일 컨테이너 안정화
- 결과: 14분 hang → 19초 성공으로 단축

## 명시적으로 위임하지 않은 부분

- 기술 스택 선택과 버전 결정
- 아키텍쳐 설계
- 비즈니스 규칙 확정 (취소 7일, 대기열 자동 승격, 정원 카운트 대상 상태)
- 격리 수준 결정과 그 영향 분석
- 컨벤션 정의 (패키지 구조, `Default` 접두사, ports.in 패턴, DTO 3계층)
- 테스트 분류와 베이스 클래스 구조
- 설계 과정 문서와 최종 제출 문서의 역할 분리

## 검증 도구

- 도메인 단위 테스트 (POJO) — 비즈니스 규칙 검증
- `@SpringBootTest` + Testcontainers MySQL 통합 테스트 — 실 DB 동작 검증
- 동시성 테스트 (`ExecutorService` + `CountDownLatch`) — race condition 검증
- 전체 테스트 통과 확인 (`./gradlew test`)
- 컴파일러 + Hibernate `ddl-auto=create`로 스키마 정합성 매 테스트 클래스마다 자동 확인
- README와 OpenAPI JSON을 실제 컨트롤러 path 기준으로 대조
