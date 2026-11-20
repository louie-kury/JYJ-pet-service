# louie-kury-pet-service

루이쿠리 반려동물 서비스의 로컬 개발 기반입니다. Spring Boot 실행 환경과 공통 응답·예외 처리, MySQL/JPA·MongoDB 감사 필드를 구성합니다. 반려동물 CRUD·업무 API, 로그인·토큰 검증, 저장소 간 트랜잭션, 배포는 이번 범위에 포함하지 않습니다.

## 개발 환경

| 항목 | 버전 / 기준 |
| --- | --- |
| Java / JDK | 17 |
| Spring Boot | 4.1.2 |
| Gradle | Wrapper 8.14.3 (`./gradlew`) |
| MySQL | 8.4.13 |
| MongoDB | Atlas 8.0.39, 개발 클러스터 `louie-kury-dev` |
| 업무·API·로그 시간대 | `Asia/Seoul` / UTC+09:00 |

접종명 기준 데이터는 MySQL, 나머지 반려동물 서비스 데이터는 MongoDB에 저장합니다. MongoDB 개발 DB 이름은 `louie_kury_pet_dev`입니다. 현재 기반 코드에는 업무 테이블·컬렉션과 Repository가 없으며 DB 이름 설정만으로 Atlas DB를 미리 생성하지 않습니다. 최초 데이터 저장 시 생성됩니다.

## 실행 준비

JDK 17을 설치하고 `JAVA_HOME`과 `PATH`를 설정합니다. macOS 예시:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

로컬 MySQL을 Docker Compose로 준비하려면 Docker Desktop을 실행하고 서비스 루트에서 다음 순서로 진행합니다.

1. `cp -n .env.example .env`로 예시를 복사합니다. 기존 `.env`는 덮어쓰지 않습니다.
2. `.env`의 `MYSQL_ROOT_PASSWORD`와 `MYSQL_PASSWORD`를 각각 로컬 개발용 비밀번호로 바꿉니다.
3. 아래 명령으로 MySQL 8.4.13을 실행하고 상태와 로그를 확인합니다. 로그에 접속 준비가 완료된 뒤 애플리케이션을 실행합니다.

```sh
docker compose up -d
docker compose ps
docker compose logs --tail=50 mysql
```

MySQL은 `127.0.0.1:3306`, DB는 `louie_kury_pet_dev`, 서비스 사용자는 `pet_service`입니다. 같은 포트나 `louie-kury-mysql` 이름을 쓰는 기존 컨테이너가 있으면 용도를 확인하고 조정하며 임의로 삭제하지 않습니다. 데이터는 `louie-kury-mysql-data` 볼륨에 남습니다. 초기화된 볼륨에서는 `.env`의 비밀번호를 바꾸는 것만으로 기존 DB 사용자 비밀번호가 변경되지 않습니다.

DB를 준비한 뒤 아래 환경변수를 현재 셸이나 IDE 실행 설정에 지정합니다. 실제 비밀번호·URI를 저장소에 저장하지 마세요. 아래 값은 형식을 설명하는 템플릿이며 그대로 접속할 수 없습니다.

| 환경변수 | 필수 | 설명 |
| --- | --- | --- |
| `MYSQL_URL` | 예 | `jdbc:mysql://<host>:3306/<database>` 형식 |
| `MYSQL_USERNAME` | 예 | MySQL 사용자 |
| `MYSQL_PASSWORD` | 예 | MySQL 비밀번호 |
| `MONGODB_URI` | 예 | Atlas에서 발급한 `mongodb+srv://<user>:<password>@<host>/?retryWrites=true&w=majority` 형식의 연결 URI |
| `MONGODB_DATABASE` | 아니요 | 기본값 `louie_kury_pet_dev` |

Atlas DB 사용자의 접근 권한과 네트워크 접근 설정을 준비해야 합니다. URI의 사용자명·비밀번호에 예약 문자가 있으면 URI 규칙에 맞게 인코딩합니다. Docker Compose는 `.env`를 자동으로 읽지만 호스트에서 실행하는 Spring Boot·IDE에는 전달하지 않습니다. `.env.example`의 MySQL 연결값과 `.env`에 설정한 서비스 비밀번호, 별도로 준비한 MongoDB URI를 애플리케이션 실행 환경변수에 지정합니다.

```sh
./gradlew bootRun
```

실행에는 두 DB 연결 설정이 필요합니다. Hibernate `ddl-auto=none`과 MongoDB 자동 인덱스 생성 비활성화를 사용하며 업무 스키마·인덱스를 자동으로 만들지 않습니다. 업무 API는 아직 없으므로 서버 기동만으로 등록·조회·인증 동작이 제공되지는 않습니다.

## 빌드와 테스트

```sh
./gradlew clean build
```

테스트만 실행하려면 `./gradlew test`를 사용합니다. 결과는 `build/reports/tests/test/index.html`에서 확인합니다. 빌드와 테스트도 위의 JDK 17 환경에서 실행합니다.

공통 코드의 테스트는 실제 DB 접속과 구분합니다. JDK 17에서 웹 공통 응답·오류 처리, 시각 변환, 감사 시각 제공과 삭제 동작의 테스트 및 컴파일·실행 JAR 생성을 확인했습니다. 자세한 결과는 PR의 검증 기록에서 확인합니다. 실제 MySQL·Atlas 연결, 감사 리스너 등록과 저장·재조회, 시간 변환의 DB 왕복 검증은 수행하지 않았습니다.

실제 DB 연결·저장 검증은 초기 검증 당시 접속 환경변수가 제공되지 않고 Docker daemon도 실행 중이지 않아 수행하지 않았습니다. 위 Compose 실행 절차 역시 컨테이너 생성·접속 성공을 검증한 결과가 아닙니다. 사용자가 Atlas에서 직접 접속·쓰기·읽기에 성공했다고 보고한 결과는 신규 서비스의 매핑·감사 기능 검증 결과로 간주하지 않습니다.

## 공통 코드와 계약

| 위치 | 역할 |
| --- | --- |
| `shared.exception` | 기술 독립 `BusinessException`·`ErrorCode` |
| `global.web.response` | `ApiResponse` 성공·오류 본문 |
| `global.web.exception` | HTTP 상태 매핑과 MVC 예외 처리 |
| `global.persistence` | JPA `BaseEntity`·MongoDB `BaseDocument` |
| `global.config` | 감사 활성화와 시각 설정 |

성공 본문은 `{"data":...}`, 오류 본문은 `{"code":"...","message":"..."}`입니다. HTTP 상태는 웹 계층에서 정하고 204 응답은 본문을 만들지 않습니다. 예상하지 못한 오류는 내부 상세를 클라이언트에 노출하지 않습니다.

감사 필드는 생성·수정·최초 삭제 시각을 관리합니다. 삭제 권한·업무상 삭제 허용 조건과 삭제 데이터 조회 제외는 이후 기능의 유스케이스·어댑터에서 구현해야 합니다. 내부 도메인은 JPA·MongoDB 기반 클래스를 상속하지 않습니다.

시각 처리의 업무 기준은 한국시간입니다. MySQL 감사 필드는 한국 벽시각을 저장하고 JDBC 연결 세션도 같은 기준을 사용합니다. MongoDB 감사 필드는 `Instant`를 사용하며 BSON Date는 UTC 기준의 순간을 저장합니다. 후속 API의 응답 DTO 팩토리에서 `ApiTime.from(...)`을 사용해 `+09:00`이 포함된 값으로 변환합니다. `spring.jackson.time-zone` 설정만으로 모든 시각 필드가 자동 변환된다고 가정하지 않습니다. MongoDB가 KST 오프셋 자체를 저장한다는 뜻은 아닙니다. 생일 같은 날짜 전용 값은 시각과 별도로 다룹니다.

MySQL 연결의 `connectionTimeZone`은 숫자 오프셋 `+09:00`을 사용하고 `forceConnectionTimeZoneToSession=true`로 세션에 적용합니다. MySQL의 이름 기반 시간대 테이블에 의존하지 않기 위한 설정입니다. Hibernate는 `hibernate.type.java_time_use_direct_jdbc=true`로 `LocalDateTime`을 직접 전달해 JVM 기본 시간대에 따른 벽시각 변환을 피합니다. 업무·API·로그의 지역 시간대 기준은 계속 `Asia/Seoul`입니다.

로그인·토큰 검증은 별도 MSA 담당자의 구현 범위입니다. 이 서비스의 인증 주체 전달·신뢰 경계와 401/403 응답 책임은 업무 API 연동 전에 합의해야 합니다. 다른 서비스가 인증을 담당한다는 이유로 임의 헤더를 신뢰하거나 무인증 접근을 허용하도록 정하지 않습니다.

## 관련 문서

- [백엔드 컨벤션](../louie-kury-document/docs/convension/BACKEND_CODE_CONVENTIONS.md)
- [API 공통 계약](../louie-kury-document/docs/api/README.md)
- [저장소와 계층 경계 결정](docs/adr/0001-storage-and-layer-boundaries.md)
- [감사 시각과 한국시간 처리](docs/adr/0002-korea-time-auditing.md)

공통 문서 링크는 형제 폴더에 `louie-kury-document`를 클론한 로컬 개발 구조를 기준으로 합니다.
