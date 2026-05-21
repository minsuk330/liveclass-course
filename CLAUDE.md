# Coding Conventions

## 패키지 구조
- `domain.{도메인}`: 엔티티 클래스 (예: `domain/liveclass`, `domain/enrollment`)
  - 공통 (`BaseEntity`, `DomainException` 등)은 `domain/common`
- `service.ports.in`: Service 인터페이스
- `service.ports.in.command.{도메인}`: Service 입력 Command (record)
- `service.ports.in.result.{도메인}`: Service 출력 Result (record)
  - 단건 상세: `XxxDetail`
  - 목록 단위: `XxxListItem`
  - 명령 결과: 보통 Entity 그대로 반환 (Result 생략)
- `service`: Service 구현 클래스 (`Default` 접두사)
- `repository`: JPA Repository + QueryDSL custom repository
- `controller.{도메인}`: REST Controller (도메인별 서브패키지)
- `controller.{도메인}.request`: Controller 입력 Request DTO (Bean Validation)
- `controller.{도메인}.response`: Controller 출력 Response DTO (정적 팩토리 `from(entity, ...)`)
- `global.error`: 예외 + 에러 코드 + 핸들러
- `global.config`: 인프라 설정 (QueryDsl, Swagger 등)

## Service
- 인터페이스는 `ports.in` 패키지에 정의
- 구현 클래스는 `service` 패키지에 `Default` 접두사 (예: `DefaultReminderListService`)
- Mock 테스트 사용 금지, `@SpringBootTest` 통합 테스트로 작성

## 테스트
- 기능 추가/수정 시 반드시 검증 테스트를 함께 작성
- 도메인 엔티티 테스트는 순수 단위 테스트 (JPA, Spring Context 의존 금지)
- Service 테스트는 `@SpringBootTest` + `@Transactional` 통합 테스트