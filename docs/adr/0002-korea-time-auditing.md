# ADR 0002: 감사 시각과 한국시간 처리

- 상태: 채택
- 날짜: 2026-11-20

## 배경

사용자는 업무 처리·API·로그를 `Asia/Seoul`(UTC+09:00) 기준으로 처리하도록 결정했다. MongoDB BSON Date는 UTC 기준 순간을 표현하므로 표시 시간대와 DB의 물리 표현을 구분해야 한다.

## 결정

- 업무용 시계와 로그 시간대를 `Asia/Seoul`로 명시한다. JVM 기본 시간대에 암묵적으로 기대지 않는다.
- JPA 감사 필드는 `LocalDateTime`으로 한국 벽시각을 표현한다. MySQL JDBC 연결은 `connectionTimeZone=+09:00`과 `forceConnectionTimeZoneToSession=true`를 사용한다. 숫자 오프셋으로 KST 세션을 적용하므로 MySQL 이름 기반 시간대 테이블에 의존하지 않는다.
- Hibernate의 `hibernate.jdbc.time_zone`은 `Asia/Seoul`로 두며 `hibernate.type.java_time_use_direct_jdbc=true`로 Java 시간 타입을 직접 전달한다. `LocalDateTime`이 JVM 기본 시간대를 거치는 Timestamp 변환으로 달라지지 않도록 한다.
- MongoDB 감사 필드는 `Instant`로 순간을 보존한다. 오프셋 없는 `LocalDateTime`의 기본 변환을 적용하지 않는다. BSON Date에 KST 오프셋 자체를 저장한다고 표현하지 않는다.
- 후속 API의 응답 DTO 팩토리는 `ApiTime.from(...)`을 사용한다. JPA 한국 벽시각에는 `Asia/Seoul`을 명시하고, MongoDB 순간은 같은 시간대로 변환하여 오프셋 포함 ISO 8601 `+09:00`으로 응답한다. Jackson 시간대 설정만으로 모든 Java 시간 타입이 자동 변환된다고 가정하지 않는다. 생일 등 날짜 전용 값은 별도로 다룬다.
- 양쪽 감사 필드는 생성·수정 시각과 최초 삭제 시각을 관리한다. 업무상 삭제 허용 조건·권한·삭제 데이터 조회 제외는 후속 기능이 담당한다.

## 영향과 검증

한국시간 날짜 경계에서 두 저장소의 변환 결과가 같은 순간을 나타내는지 검증한다. 시각 변환·감사 이벤트 단위 검증은 실제 MySQL·MongoDB 저장 후 재조회와 구분한다. 실제 DB 연결 환경이 없으면 DB 왕복 검증 미실행을 명시한다. BSON Date의 정밀도는 밀리초이므로 저장·재조회 비교에서 나노초 보존을 가정하지 않는다.
