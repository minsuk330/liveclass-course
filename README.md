# 라이브클래스 수강 신청 시스템

## 프로젝트 개요

크리에이터(강사)가 강의를 개설하고, 클래스메이트(수강생)가 수강 신청, 결제, 취소, 대기열 등록을 수행하는 백엔드 API입니다.

과제의 요구사항인 CRUD와 정원 초과 방지, 상태 전이, 결제 확정, 취소 가능 기간, 대기열 승격, 페이지네이션을 함께 다룹니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.6
- Spring Data JPA
- MySQL 8.4
- Querydsl 7.1
- Testcontainers
- springdoc-openapi 3.0.3
- Docker Compose

## 실행 방법

Docker Compose로 MySQL과 애플리케이션을 함께 실행합니다.

```bash
docker compose up --build
```

백그라운드 실행:

```bash
docker compose up --build -d
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

기본 DB 설정:

```text
host: localhost
port: 3311
database: course
username: course
password: course
```

로컬 Java 17 환경에서 직접 실행하려면 MySQL만 먼저 실행한 뒤 애플리케이션을 실행합니다.

```bash
docker compose up -d mysql
./gradlew bootRun
```

## 요구사항 해석 및 가정

### 1. 강의 관리

- 상태 전이는 `DRAFT -> OPEN -> CLOSED`만 허용하며, `CLOSED` 이후 재오픈은 지원하지 않는다.
- 강의 시작일은 오늘 또는 미래만 허용하며, `endDate`는 `startDate` 이후여야 한다.
- 강의 삭제는 물리 삭제가 아니라 soft delete로 처리한다.
- `PENDING` 또는 `CONFIRMED` 신청이 남아 있는 강의는 삭제할 수 없다.
- 강의를 삭제하면 해당 강의의 대기열 항목도 함께 soft delete한다.
- 크리에이터 전용 기능은 실제 인증/인가 대신 `creatorId`와 `LiveClass.creator.id` 비교로 검증한다.

### 2. 수강 신청 관리

- 수강 신청은 `OPEN` 상태의 강의에만 가능하다.
- 수강 신청 직후 상태는 `PENDING`이며, 결제 확정 후 `CONFIRMED`가 된다.
- `PENDING` 신청은 결제 전 상태이므로 기간 제한 없이 취소 가능하다.
- `CONFIRMED` 신청은 결제 후 7일 이내에만 취소 가능하다.
- 취소 가능 기간은 서버 JVM 기본 시간대를 기준으로 계산한다.
- 같은 사용자의 동일 강의 `CANCELLED` row가 있어도 새 `PENDING` 신청 생성을 허용한다.
- 사용자 본인 검증은 실제 로그인 세션 대신 `userId` 요청 파라미터로 처리한다.

### 3. 정원 관리 규칙

- 정원 계산에는 `PENDING`, `CONFIRMED` 상태의 신청만 포함한다.
- `CANCELLED` 신청은 정원을 차지하지 않는다.
- 같은 사용자는 같은 강의에 대해 유효한 신청(`PENDING`, `CONFIRMED`)을 중복 생성할 수 없다.
- 수강 신청과 대기열 등록은 강의 row 비관적 락을 기준으로 처리한다.

### 4. 결제 처리

- 실제 PG 연동은 하지 않고 `MockNicePgClient`로 NICE PG 흐름을 모사한다.
- 결제 흐름은 `READY -> IN_PROGRESS -> PAID`를 기본 성공 경로로 둔다.
- 결제 시작 시점의 강의 가격을 `Payment.amount`에 snapshot으로 저장한다.
- 결제 수단은 현재 `CARD`만 사용한다.
- 동일 수강 신청에 active 결제(`READY`, `IN_PROGRESS`, `PAID`)가 있으면 새 결제를 시작할 수 없다.
- 결제 실패(`FAILED`) 또는 취소(`CANCELLED`) 상태는 새 결제 시도로 재시도할 수 있다고 해석했다.
- 결제 callback은 PG webhook 성격의 요청으로 보고 별도 인증은 생략했다.
- 결제 승인(confirm)은 Payment row 비관적 락으로 중복 승인 race를 차단한다.

### 5. 대기열과 목록 조회

- 대기열은 강의 정원이 이미 찬 경우에만 등록 가능하다.
- 이미 active 신청이 있는 사용자는 같은 강의의 대기열에 등록할 수 없다.
- 같은 사용자는 같은 강의에 대해 active 대기열 항목을 중복 생성할 수 없다.
- 대기열 취소는 soft delete로 처리하며, 취소 후 재등록 가능하도록 `activeUserId`를 별도로 둔다. 삭제된 row는 `activeUserId`를 `NULL`로 바꿔 active unique 제약 대상에서 제외한다.
- 확정 수강생이 취소하면 대기열 첫 번째 사용자를 `PENDING` 신청으로 자동 승격한다.
- 정렬 필드는 enum으로 제한한다.
- 페이지네이션 응답은 `total`, `page`, `size`, `data` 형태로 통일한다.

### 6. 공통 응답과 에러

- 에러 응답은 `code`, `message`, `fieldErrors`, `meta` 형식으로 통일한다.
- `meta`는 정원 초과 시 대기열 안내처럼 추가 정보가 필요한 경우에만 사용한다.
- 사용자에게 내려가는 에러 메시지는 한국어를 기본으로 한다.

## 설계 결정과 이유

### JPA 연관관계

`Enrollment`, `Payment`, `WaitlistEntry`는 `ManyToOne(fetch = LAZY)`로 상위 도메인을 참조합니다.

- `LiveClass.creator -> User`
- `Enrollment.liveClass -> LiveClass`
- `Enrollment.user -> User`
- `Payment.enrollment -> Enrollment`
- `WaitlistEntry.liveClass -> LiveClass`
- `WaitlistEntry.user -> User`

반대로 `LiveClass.enrollments` 같은 `OneToMany` 컬렉션은 두지 않았습니다. 수강생 목록과 신청 목록은 페이징이 필요하므로 컬렉션 탐색보다 repository 조회가 더 적합하다고 판단했습니다.

### Soft Delete와 도메인 상태 분리

모든 엔티티는 `BaseEntity.deletedAt`을 통해 soft delete를 지원합니다. `deletedAt`은 도메인 상태가 아니라 운영상 일반 조회에서 숨기기 위한 필드입니다.

도메인 이벤트 시각은 별도 필드로 분리했습니다.

- 수강 취소: `Enrollment.cancelledAt`
- 결제 승인: `Payment.approvedAt`
- soft delete: `BaseEntity.deletedAt`

대기열은 `class_id`, `active_user_id` 조합으로 active 중복을 제한합니다. soft delete 시 `activeUserId`를 `NULL`로 바꿔 삭제된 row가 재등록을 막지 않도록 했습니다.

### 정원 동시성 제어

정원이 차는 시점에 여러 사용자가 동시에 마지막 자리에 신청하는 시나리오를 어떻게 막을지가 본 시스템의 핵심 문제라 생각했습니다.

낙관적 락, atomic update, 비관적 락을 검토했고, 본 프로젝트에서는 `LiveClass` row에 **비관적 락**을 거는 방식을 선택했습니다.

낙관적 락은 경합이 적을 때 성능상 유리하지만, 충돌이 발생하면 retry/backoff 정책을 별도로 설계해야 합니다. 특히 마지막 한 자리 신청처럼 충돌 가능성이 높은 시나리오에서는 자동 retry가 사용자 의사와 다르게 자리를 차지할 수 있고, 수동 retry는 사용성을 떨어뜨릴 수 있다고 판단했습니다.

atomic update 방식은 `UPDATE ... WHERE remaining > 0` 형태로 처리할 수 있어 성능상 유리합니다. 다만 이를 위해서는 `remaining` 또는 `currentEnrollmentCount` 같은 카운터 컬럼을 별도로 관리해야 합니다. 현재 도메인은 `PENDING`/`CONFIRMED` 상태 전이, 수강 취소, 대기열 자동 승격이 함께 발생하므로, 카운터 컬럼이 실제 `Enrollment` row count와 어긋나지 않도록 계속 정합성을 보장해야 합니다.

이번 과제에서는 대규모 트래픽 최적화보다 정원 초과 방지 규칙을 명확하게 표현하고 테스트로 검증하는 것을 우선했습니다. 그래서 성능상 더 유리할 수 있는 atomic update 대신, JPA가 표준으로 제공하는 `PESSIMISTIC_WRITE`를 사용해 `LiveClass` row 기준으로 신청 처리를 직렬화했습니다.

이 방식은 별도 인프라 없이 DB 트랜잭션만으로 구현할 수 있고, Testcontainers MySQL + `ExecutorService` 다중 thread 동시 호출 시나리오로 검증하기도 쉽습니다 (`EnrollmentConcurrencyTest`, `WaitlistConcurrencyTest`, `PaymentConcurrencyTest`).

실제 락 사용 흐름은 다음과 같습니다.

```text
LiveClass SELECT FOR UPDATE      ← 트랜잭션 첫 read여야 함
→ 강의 상태 확인 (OPEN인지)
→ 사용자 중복 신청 확인
→ active 신청 수 count (PENDING + CONFIRMED)
→ capacity 미만이면 신청 저장, 초과면 대기열 안내 (meta 응답)
```

#### 적용 지점 정리

| 지점 | 락 대상 | 격리 수준 | 비고 |
|---|---|---|---|
| 수강 신청 (`POST /enrollments`) | `LiveClass` row | 기본 (`REPEATABLE_READ`) | 락이 메서드 첫 statement |
| 대기열 등록 (`POST /classes/{id}/waitlist`) | `LiveClass` row | 기본 | 락이 메서드 첫 statement |
| 결제 승인 (`POST /payments/{id}/confirm`) | `Payment` row | 기본 | 같은 결제의 중복 승인 race 방지 |
| 수강 취소 + 대기열 자동 승격 (`DELETE /enrollments/{id}`) | `LiveClass` row | **`READ_COMMITTED`** | enrollment 조회로 `classId`를 먼저 알아야 락 가능 → 락 이전 read 존재 → snapshot 함정 회피 필요 |

#### REPEATABLE_READ snapshot 함정

InnoDB 기본 격리 수준은 `REPEATABLE_READ`이며 **트랜잭션의 첫 consistent read 시점에 snapshot이 고정**됩니다. `SELECT ... FOR UPDATE` 같은 locking read는 최신 커밋을 보는 current read이지만, 그 전에 일반 SELECT가 먼저 실행되어 read view가 만들어지면 이후의 일반 SELECT는 기존 snapshot을 볼 수 있습니다.

예: 같은 강의의 확정 수강생 두 명이 동시에 취소하면, 각 트랜잭션이 `enrollment`를 먼저 조회하면서 snapshot이 고정될
수 있습니다. 이후 `LiveClass` row lock을 순서대로 획득하더라도, 대기열 조회가 기존 snapshot을 기준으로 수행되면 같은
대기열 1번을 보고 같은 사용자를 중복 승격할 위험이 있습니다.

해결 방향은 두 가지였습니다.

1. **`enrollmentId` 외에 `classId`도 path/요청에서 받아 락을 첫 statement로 둔다** — REST 인터페이스가 어색해짐
2. **`READ_COMMITTED` 격리 수준** — 매 SELECT마다 최신 커밋 데이터를 읽어 snapshot 고정 문제를 피함

2번을 선택했습니다. 격리 수준 변경의 영향은 본 메서드 범위(취소 + 승격)에 한정됩니다. 또한 동일 강의의 취소/승격 흐름은 `LiveClass` row lock으로 직렬화되므로, 이 메서드에서 문제 되는 중복 승격은 방지됩니다.

### 결제 흐름

결제는 실제 PG 대신 mock PG를 사용하지만, 흐름은 NICE PG 방식에 맞춰 분리했습니다.

```text
Ready
→ Callback
→ Confirm
```

- Ready: `Payment(READY)` 생성, `tid`, `paymentUrl` 발급
- Callback: PG 인증 결과 수신, `READY -> IN_PROGRESS` 또는 `FAILED`
- Confirm: 서버가 PG 승인 요청, 성공 시 `PAID` 및 `Enrollment.CONFIRMED`

`Confirm`은 `Payment` row에 비관적 락을 걸어 같은 결제의 중복 승인 race를 막습니다.

### Querydsl과 페이지네이션

강의 목록과 수강 신청 목록은 Querydsl 기반 custom repository로 조회합니다.

페이지 요청은 `PageOptions`로 공통화했습니다.

```text
page: 0부터 시작
size: 기본 10, 최대 100
```

정렬은 enum으로 제한합니다.

- 강의 목록: `ClassSortType`
- 내 수강 신청 목록: `EnrollmentSortType`
- 방향: `SortDirection`

강의 목록의 현재 신청 인원은 각 강의마다 count 쿼리를 날리지 않고, 현재 페이지의 강의 ID 목록으로 한 번에 집계합니다.

```text
강의 페이지 조회 1회
현재 페이지 강의들의 active 신청 수 집계 1회
```

## API 목록 및 예시

아래 목록과 샘플은 컨트롤러 기준입니다. 전체 요청/응답 스키마는 실행 후 Swagger UI 또는 [docs/openapi.json](docs/openapi.json)에서 확인할 수 있습니다.

주요 API:

| 기능 | Method | Path |
|---|---:|---|
| 강의 등록 | POST | `/api/v1/classes?creatorId={id}` |
| 강의 상세 조회 | GET | `/api/v1/classes/{classId}` |
| 강의 목록 조회 | GET | `/api/v1/classes` |
| 강의 상태 변경 | PATCH | `/api/v1/classes/{classId}/status` |
| 강의 삭제 | DELETE | `/api/v1/classes/{classId}` |
| 강의별 수강생 목록 | GET | `/api/v1/classes/{classId}/enrollments` |
| 수강 신청 | POST | `/api/v1/enrollments` |
| 내 수강 신청 목록 | GET | `/api/v1/users/{userId}/enrollments` |
| 수강 취소 | DELETE | `/api/v1/enrollments/{enrollmentId}` |
| 결제 시작 | POST | `/api/v1/enrollments/{enrollmentId}/payments` |
| 결제 callback | POST | `/api/v1/payments/callback` |
| 결제 승인 | POST | `/api/v1/payments/{paymentId}/confirm` |
| 대기열 등록 | POST | `/api/v1/classes/{classId}/waitlist` |
| 내 대기열 조회 | GET | `/api/v1/users/{userId}/waitlist` |
| 대기열 취소 | DELETE | `/api/v1/waitlist/{waitlistId}` |

### 샘플 요청/응답

강의 등록:

```http
POST /api/v1/classes?creatorId=1
Content-Type: application/json

{
  "title": "Spring JPA 실전",
  "description": "수강 신청 시스템 구현",
  "price": 50000,
  "capacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30"
}
```

```json
{
  "id": 1,
  "title": "Spring JPA 실전",
  "description": "수강 신청 시스템 구현",
  "price": 50000,
  "capacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "status": "DRAFT",
  "creatorId": 1,
  "currentEnrolled": 0,
  "availableSeats": 30,
  "createdAt": "2026-05-24T10:00:00",
  "updatedAt": "2026-05-24T10:00:00"
}
```

수강 신청:

```http
POST /api/v1/enrollments?userId=2
Content-Type: application/json

{
  "classId": 1
}
```

```json
{
  "enrollmentId": 1,
  "classId": 1,
  "userId": 2,
  "status": "PENDING",
  "createdAt": "2026-05-24T10:05:00",
  "paidAt": null,
  "cancelledAt": null
}
```

결제 시작:

```http
POST /api/v1/enrollments/1/payments?userId=2
```

```json
{
  "paymentId": 1,
  "tid": "nicepay_abc123",
  "paymentUrl": "https://mock-nicepay.test/pay/nicepay_abc123",
  "amount": 50000
}
```

페이지 응답 형식:

```json
{
  "total": 1,
  "page": 0,
  "size": 10,
  "data": []
}
```

## 데이터 모델 설명

DB 스키마 SQL은 [docs/DB_스키마.sql](docs/DB_스키마.sql)에 포함했습니다.

### User

서비스 사용자를 표현합니다.

주요 필드:

- `id`: 사용자 식별자
- `name`: 사용자 이름
- `role`: 사용자 역할

역할:

- `CREATOR`: 강의 개설자
- `CLASSMATE`: 수강생

### LiveClass

크리에이터가 개설한 강의입니다.

주요 필드:

- `title`: 강의 제목
- `description`: 강의 설명
- `price`: 강의 가격
- `capacity`: 최대 수강 인원
- `startDate`, `endDate`: 수강 기간
- `status`: 강의 상태
- `creator`: 강의를 개설한 사용자

상태:

- `DRAFT`: 초안, 신청 불가
- `OPEN`: 모집 중, 신청 가능
- `CLOSED`: 모집 마감, 신청 불가

### Enrollment

사용자의 수강 신청입니다.

주요 필드:

- `liveClass`: 신청한 강의
- `user`: 신청 사용자
- `status`: 신청 상태
- `paidAt`: 결제 확정 시각
- `cancelledAt`: 취소 시각

상태:

- `PENDING`: 신청 완료, 결제 대기
- `CONFIRMED`: 결제 완료, 수강 확정
- `CANCELLED`: 취소됨

### Payment

수강 신청에 대한 결제입니다.

주요 필드:

- `enrollment`: 결제 대상 수강 신청
- `amount`: 결제 시작 시점의 강의 가격 snapshot
- `status`: 결제 상태
- `method`: 결제 수단
- `tid`: PG 거래 ID
- `authToken`: PG 인증 token
- `approvedAt`: 승인 시각
- `failedReason`: 실패 사유

상태:

- `READY`: 결제 시작
- `IN_PROGRESS`: PG 인증 성공, 승인 대기
- `PAID`: 승인 완료
- `FAILED`: 실패
- `CANCELLED`: 취소

### WaitlistEntry

정원이 찬 강의에 대한 대기열 항목입니다.

주요 필드:

- `liveClass`: 대기 중인 강의
- `user`: 대기 사용자
- `position`: 대기 순번
- `activeUserId`: soft delete 후 같은 사용자에 대한 재등록을 허용하기 위한 active unique key

## 테스트 실행 방법

테스트는 Testcontainers MySQL을 사용하므로 **Docker가 실행 중이어야 합니다**.

### 전체 테스트

```bash
./gradlew test
```

- 총 **120 테스트** / 12 클래스 / 평균 약 20초
- 결과 리포트: `build/reports/tests/test/index.html`

### 테스트 분류

| 분류 | 대상 | 환경 |
|---|---|---|
| 도메인 단위 | `ClassStatusTest`, `LiveClassTest`, `EnrollmentTest`, `PaymentTest` | POJO, Spring/DB 없음 |
| 서비스 통합 | `Default*ServiceTest` | `@SpringBootTest` + Testcontainers MySQL + `@Transactional` 자동 롤백 |
| 동시성 | `*ConcurrencyTest` | 메서드 내부 `ExecutorService` + `@AfterEach` TRUNCATE |
| 컨텍스트 로딩 | `CourseApplicationTests` | Spring context 부팅 확인 |


---

### Docker 컨테이너 안에서 테스트 실행

Docker 환경에서 직접 테스트를 돌리려는 경우입니다.

**전제 — sibling container 패턴**

- 컨테이너 안에서 `gradle test`를 실행
- Testcontainers가 호스트 Docker 데몬에 명령을 보내 MySQL 컨테이너를 **sibling**(자식 아님)으로 띄움
- 따라서 호스트의 `/var/run/docker.sock`을 컨테이너에 mount해야 합니다

#### macOS / Windows (Docker Desktop)

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -v "$HOME/.gradle":/root/.gradle \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -w /workspace \
  eclipse-temurin:17-jdk \
  ./gradlew test --no-daemon
```

#### Linux (Docker Engine)

```bash
docker run --rm \
  --network host \
  -v "$PWD":/workspace \
  -v "$HOME/.gradle":/root/.gradle \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace \
  eclipse-temurin:17-jdk \
  ./gradlew test --no-daemon
```


## 미구현 / 제약사항

- 실제 PG 연동은 구현하지 않고 `MockNicePgClient`로 대체했습니다.
- 인증/인가 시스템은 실제 로그인 세션 대신 `userId`, `creatorId` 요청 파라미터로 단순화했습니다.
- 결제 취소/환불 API는 구현 범위에서 제외했습니다.
- `ddl-auto=create-drop` 설정이므로 애플리케이션 재시작 시 데이터가 초기화됩니다.

## AI 활용 범위

AI는 요구사항 분석, API 명세 초안, README 초안, 테스트 케이스 아이디어, 구현 방향 검토에 활용했습니다.

AI가 제안한 내용은 실제 코드와 대조해 수정했으며, 도메인 테스트, 서비스 통합 테스트, 동시성 테스트로 검증 가능한 형태로 반영했습니다.

상세 기록은 `docs/AI_USAGE.md`에 정리했습니다.


## 제출 보조 문서

BE 과제 추가 제출물은 아래 파일을 기준으로 확인할 수 있습니다.

- [API 명세](docs/openapi.json): 애플리케이션의 `/v3/api-docs`에서 생성한 OpenAPI JSON 명세
- [DB 스키마](docs/DB_스키마.sql): JPA 엔티티 기준으로 생성된 MySQL DDL
