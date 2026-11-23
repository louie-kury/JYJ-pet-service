# louie-kury-pet-service

루이쿠리 반려동물 서비스입니다. Spring Boot·MySQL/JPA·MongoDB 기반 위에 반려동물 등록 API를 제공합니다. 이번 범위는 무인증 등록이며 회원·로그인·소유자 연결, 목록·상세 조회·수정·삭제와 배포는 후속 작업입니다.

## 개발 환경

| 항목 | 버전 / 기준 |
| --- | --- |
| Java / JDK | 17 |
| Spring Boot | 4.1.2 |
| Gradle | Wrapper 8.14.3 (`./gradlew`) |
| MySQL | 8.4.13 |
| MongoDB | Atlas 8.0.39, 개발 클러스터 `louie-kury-dev` |
| 업무·API·로그 시간대 | `Asia/Seoul` / UTC+09:00 |

접종명 기준 데이터는 MySQL, 나머지 반려동물 서비스 데이터는 MongoDB에 저장합니다. MongoDB 개발 DB 이름은 `louie_kury_pet_dev`입니다. 등록 기본정보는 `pets`, 종·품종 기준은 `pet_species` 컬렉션을 사용합니다. DB 이름 설정만으로 기준 데이터·필수 인덱스가 준비되지는 않습니다.

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

실행에는 두 DB 연결 설정이 필요합니다. Hibernate `ddl-auto=none`과 MongoDB 자동 인덱스 생성 비활성화를 사용하며 업무 스키마·인덱스를 자동으로 만들지 않습니다. 등록 전에는 아래 MongoDB 기준 데이터·인덱스 준비가 필요합니다. 서버 기동은 DB 저장 성공을 검증한 결과가 아닙니다.

## 반려동물 등록

`POST /api/pets`는 현재 인증 없이 등록합니다. 회원 기능이 없어 `caregiverId`를 null로 저장하며 본문·임의 헤더의 사용자 ID를 소유자로 사용하지 않습니다. 기존 미연결 데이터의 귀속은 인증 도입 때 별도로 정합니다. [임시 결정 ADR](docs/adr/0003-registration-before-member-linking.md)을 참고하세요.

이름·종·성별은 필수이며 나머지 기본정보는 생략/null을 허용합니다. 이름은 공백 제거 후 1~50자, 성별은 `MALE`/`FEMALE`, 체중은 kg 단위로 0 초과 200 이하·유효 소수 최대 3자리입니다. 생일은 한국시간 오늘 이후를 거부합니다. 입력·나이 계산의 상세 기준은 [개발 계획](docs/plans/pet-registration.md)에 있습니다.

다음 요청은 형식 예시입니다. 아래 MongoDB 준비 스크립트의 기본 강아지 ID `dog`를 사용합니다. 기존 데이터에 적용하기 전 ID/code 매핑을 확인하세요. 실제 서버 QA에서도 기본 종을 이용한 무인증 등록 성공을 확인했습니다.

```sh
curl -i -X POST http://localhost:8080/api/pets \
  -H 'Content-Type: application/json' \
  -d '{"name":"루이","speciesId":"dog","sex":"MALE"}'
```

성공하면 `201 Created`, `Location: /api/pets/{petId}`와 `{"data": ...}` 형식의 등록 상세를 반환합니다. Location의 상세 GET은 이번 범위에서 제공하지 않습니다. 생일 미상이면 나이는 null이고 감사 시각은 `+09:00`으로 응답합니다.

`profileMediaId`가 생략/null이면 등록할 수 있습니다. non-null은 미디어 검증·연결 계약이 없어 저장 전 `503 MEDIA_SERVICE_UNAVAILABLE`로 거부합니다. 파일을 확인한 것처럼 성공시키지 않습니다.

선택 헤더 `Idempotency-Key`는 ASCII 영숫자·`-`·`_`·`.`·`:`의 1~128자입니다. 현재 익명 등록 전체에서 같은 키는 입력이 같거나 달라도 `409 IDEMPOTENCY_CONFLICT`이며 기존 ID·응답을 재전달하지 않습니다. 무키 요청은 각각 새 동물이므로 응답 유실 후 무키 재시도의 중복을 막지 않습니다. 이는 공통 API 초안의 사용자별 결과 재전달과 다른 임시 정책입니다.

등록 전에 대상 DB를 확인하고 서비스 루트에서 준비·확인 스크립트를 명시적으로 실행합니다. 다음은 이미 설정한 `MONGODB_URI`에 접속해 개발 DB를 선택하는 예시입니다. 다른 DB를 사용할 때 두 명령의 DB 이름을 함께 바꿉니다.

```sh
mongosh "$MONGODB_URI" --eval 'db = db.getSiblingDB("louie_kury_pet_dev"); load("scripts/mongodb/prepare-registration.js");'
mongosh "$MONGODB_URI" --eval 'db = db.getSiblingDB("louie_kury_pet_dev"); load("scripts/mongodb/verify-registration.js");'
```

`prepare-registration.js`는 `pets.registration.requestKey`의 `registration_request_key_unique` partial unique 인덱스와 종 code unique 인덱스를 만들고 기본 종 `dog`/`DOG`·`cat`/`CAT`을 없을 때만 삽입합니다. 기존 종·품종을 덮어쓰지 않고 타입·종 code·품종 ID 중복 및 기본 ID/code 충돌을 검사합니다. 기본 품종 배열은 비어 있으므로 초기 등록은 `breedId`를 생략/null로 보냅니다. 실제 품종을 사용할 때는 검토된 데이터를 해당 종의 `breeds` 배열에 `breedId`, `name`, `active` 형태로 준비합니다.

`verify-registration.js`는 키 인덱스의 이름·필드·unique·partial 조건과 활성 DOG/CAT 기준을 확인합니다. 키 있는 등록은 저장 직전에도 이 인덱스를 검사하고 없거나 정의가 다르면 503으로 중단합니다. 문자열 키가 없는 문서는 unique 대상에서 제외돼 무키 다건 등록이 가능합니다. 자동 인덱스 생성·기존 데이터 삭제·운영 DB 자동 시딩은 하지 않습니다. 스크립트 실행·실제 DB 검증 결과는 아래 검증 기록과 구분해 확인합니다.

## 빌드와 테스트

```sh
./gradlew clean build
```

테스트만 실행하려면 `./gradlew test`를 사용합니다. 결과는 `build/reports/tests/test/index.html`에서 확인합니다. 빌드와 테스트도 위의 JDK 17 환경에서 실행합니다.

2026-11-23 JDK 17에서 다음 검증 명령이 성공했습니다. 이 주소·포트는 검증 사례이며 고정 실행 설정이 아닙니다.

```sh
PET_TEST_MONGODB_URI=mongodb://127.0.0.1:27028 ./gradlew test build
```

전체 101개 테스트가 실패·건너뜀 없이 통과했으며 실제 MongoDB를 사용하는 11개 검사를 포함합니다. JSON 소수 원문이 부정확하게 해석되던 두 사례와 응답·저장 시각의 밀리초 정밀도 불일치 사례를 수정한 회귀 검사도 통과했습니다. 독립 코드 리뷰에서 남은 지적은 없었습니다.

실행 JAR을 표준 프로필로 기동해 로컬 MongoDB 8.0·MySQL 8.4.13 연결을 확인했고, 별도 QA DB에 준비·확인 스크립트를 적용했습니다. 실제 서버의 독립 QA도 통과했습니다. MySQL 연결 확인은 접종명 업무 구현·저장 검증을 뜻하지 않으며 Atlas 검증 결과로 대신하지 않습니다.

독립 QA는 실제 서버에서 39개 시나리오·42개 요청과 최초 정상 등록 1건을 확인했습니다. 정상 저장 10건의 소유자 명시 null·생일 문자열·Decimal128·스냅샷/감사 시각 일치를 확인했고, 동일 키 동시 4건은 201 한 건·409 세 건으로 처리됐습니다. 입력·사용자 ID 주입·종/품종 오류, 미디어 non-null의 503, 미세 소수 경계 거부와 실패 시 미저장도 확인했습니다. 남은 결함은 없었습니다.

회원·인증·소유권 연결과 실제 미디어 서비스 연동은 이번 구현 범위에서 제외했습니다. 무인증 등록·미연결 데이터 및 미디어 값 거부의 현재 계약을 검증하며, 인증·파일 연동 완료로 표현하지 않습니다.

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

시각 처리의 업무 기준은 한국시간입니다. MySQL 감사 필드는 한국 벽시각을 저장하고 JDBC 연결 세션도 같은 기준을 사용합니다. MongoDB 감사 필드는 `Instant`를 사용하며 BSON Date는 UTC 기준의 순간을 저장합니다. 후속 API의 응답 DTO 팩토리에서 `ApiTime.from(...)`을 사용해 `+09:00`이 포함된 값으로 변환합니다. `spring.jackson.time-zone` 설정만으로 모든 시각 필드가 자동 변환된다고 가정하지 않습니다. MongoDB가 KST 오프셋 자체를 저장한다는 뜻은 아닙니다. 생일은 MongoDB 문자열 `YYYY-MM-DD`로 저장합니다. 등록의 생성·수정 Instant는 BSON Date와 일치하도록 밀리초로 절삭하고, BaseDocument와 완료 결과 스냅샷에 같은 시각을 넣어 단일 insert합니다. 이 경로에서는 감사 콜백으로 시각을 다시 생성하지 않습니다.

MySQL 연결의 `connectionTimeZone`은 숫자 오프셋 `+09:00`을 사용하고 `forceConnectionTimeZoneToSession=true`로 세션에 적용합니다. MySQL의 이름 기반 시간대 테이블에 의존하지 않기 위한 설정입니다. Hibernate는 `hibernate.type.java_time_use_direct_jdbc=true`로 `LocalDateTime`을 직접 전달해 JVM 기본 시간대에 따른 벽시각 변환을 피합니다. 업무·API·로그의 지역 시간대 기준은 계속 `Asia/Seoul`입니다.

로그인·토큰 검증은 별도 MSA 담당자의 구현 범위입니다. 현재 등록은 사용자 요청에 따라 무인증으로 허용하며, 이는 다른 업무 API의 접근 정책을 결정하지 않습니다. 인증 도입 시 검증된 주체 전달·소유권·401/403 책임과 기존 미연결 동물 귀속을 별도 구현합니다. 임의 헤더는 신뢰하지 않습니다.

## 관련 문서

- [백엔드 컨벤션](../louie-kury-document/docs/convension/BACKEND_CODE_CONVENTIONS.md)
- [API 공통 계약](../louie-kury-document/docs/api/README.md)
- [저장소와 계층 경계 결정](docs/adr/0001-storage-and-layer-boundaries.md)
- [감사 시각과 한국시간 처리](docs/adr/0002-korea-time-auditing.md)

공통 문서 링크는 형제 폴더에 `louie-kury-document`를 클론한 로컬 개발 구조를 기준으로 합니다.
