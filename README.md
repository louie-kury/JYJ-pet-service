# louie-kury-pet-service

루이쿠리 반려동물 서비스입니다. 반려동물 등록과 목록·상세 조회에는 인증이 필요합니다. 수정은 소유자 미연결 데이터에 대한 임시 방식이며 기본적으로 비활성화됩니다. 인증된 수정과 삭제는 후속 작업입니다.

## 개발 환경

| 항목 | 버전 / 기준 |
| --- | --- |
| Java / JDK | 17 |
| Spring Boot | 4.1.2 |
| Gradle | Wrapper 8.14.3 (`./gradlew`) |
| MySQL | 8.4.13 |
| MongoDB | Atlas 8.0.39, 개발 클러스터 `louie-kury-dev` |
| MongoDB 마이그레이션 | Liquibase core·MongoDB 확장 5.0.4, 별도 MongoDB 드라이버 5.9.1 |
| 업무·API·로그 시간대 | `Asia/Seoul` / UTC+09:00 |

접종명 기준 데이터는 MySQL, 나머지 반려동물 서비스 데이터는 MongoDB에 저장합니다. MongoDB 개발 DB 이름은 `louie_kury_pet_dev`입니다. 등록 기본정보는 `pets` 컬렉션에 저장합니다. 종은 `PetSpecies` enum의 DOG/CAT을 기준으로 하며 외부 요청·응답·저장 ID는 기존 `dog`/`cat`을 유지합니다. `pet_species`는 기존 품종 배열을 조회하는 호환 저장소로 사용합니다. DB 이름 설정만으로 마이그레이션 이력·필수 인덱스가 준비되지는 않습니다.

등록·목록·상세 조회의 인증에는 `pet.auth.jwks-uri`, `pet.auth.issuer`, `pet.auth.audience` 설정이 필요합니다. 요청에는 `Authorization: Bearer <accessToken>`을 전달합니다. 등록은 인증된 회원 ID를 소유자로 저장하고 목록은 본인의 미삭제 동물만 반환합니다. 기존 소유자 미연결 데이터는 목록에서 제외됩니다.

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

앱 기동 전 MongoDB 마이그레이션을 먼저 실행합니다. 마이그레이션 명령에는 아래 전용 설정만 필요하며 MySQL 연결은 요구하지 않습니다. 실제 URI는 보안된 셸/실행 환경에 지정하고 `.env`를 자동으로 읽는다고 가정하지 않습니다.

| 마이그레이션 환경변수 | 필수 | 설명 |
| --- | --- | --- |
| `MONGODB_MIGRATION_URI` | 예 | 마이그레이션 계정 URI. URI에 DB가 있으면 아래 이름과 일치해야 합니다. |
| `MONGODB_MIGRATION_DATABASE` | 예 | 대상 DB 명시. 개발 예시는 `louie_kury_pet_dev`입니다. |

```sh
./gradlew mongoMigrationStatus
./gradlew mongoMigrate
./gradlew mongoMigrationStatus
./gradlew bootRun
```

`mongoMigrate`의 `MIGRATION_COMPLETE`를 확인한 뒤 앱을 기동합니다. status는 체크섬을 검증하고 미적용 변경 수·ID를 출력하며 앱 실행 준비 상태 전체를 대신 검사하지는 않습니다. 마이그레이션 전용 계정에는 대상 DB의 이력/락 읽기쓰기, 기준 데이터 읽기/추가 및 인덱스 생성 권한이 필요합니다. 앱 계정은 업무 권한 외에 이력/락·구조 읽기 권한이 필요하며 앱이 직접 DDL을 실행하지 않습니다.

앱 실행에는 앞서 안내한 MySQL·MongoDB 설정이 모두 필요합니다. Hibernate `ddl-auto=none`과 MongoDB 자동 인덱스 생성 비활성화를 유지합니다. 서버는 시작 시 완료 이력·활성 락 부재·등록 키 인덱스를 읽기 전용으로 검사하고 미준비·조회 실패면 기동을 중단합니다. 현재 앱 기동은 과거 종 seed·활성 상태에 의존하지 않습니다. 체크섬의 실제 재계산·변조 거부는 별도 migration/status 명령의 책임이므로 배포 시 해당 검증을 생략하지 않습니다.

## 빌드와 테스트

```sh
./gradlew clean build
```

테스트만 실행하려면 `./gradlew test`를 사용합니다. 결과는 `build/reports/tests/test/index.html`에서 확인합니다. 빌드와 테스트도 위의 JDK 17 환경에서 실행합니다.

실제 MongoDB 통합 검사를 포함하려면 `PET_TEST_MONGODB_URI`를 테스트 전용 MongoDB URI로 지정한 뒤 `./gradlew test build`를 실행합니다. 설정하지 않으면 MongoDB 통합 검사는 건너뜁니다. 테스트는 전용 DB를 생성·정리하므로 개발·운영 데이터 계정을 그대로 사용하지 않습니다. Mock·웹 검사의 통과와 실제 MongoDB·실행 서버 검증은 구분합니다.

실제 미디어 서비스 연동과 인증된 수정은 아직 구현하지 않았습니다. MySQL용 마이그레이션은 후속 작업입니다.
