# 반려동물 등록 기능 개발 계획

- 작성일: 2026-11-20 / 변경일: 2026-11-23
- 상태: `feat/5-create-pet`에서 구현·검증 완료. 커밋·push·PR은 포함하지 않았다.
- 대상: `louie-kury-pet-service`, 구현 브랜치 `feat/5-create-pet`, 기준 `dev`
- 기능: PET-01 중 `POST /api/pets`
- 근거: [반려동물 API](../../../louie-kury-document/docs/api/pet.md), [API 공통 계약](../../../louie-kury-document/docs/api/README.md), [미디어 API](../../../louie-kury-document/docs/api/media.md), [PRD](../../../louie-kury-document/docs/prd.md), [ERD](../../../louie-kury-document/docs/erd.md), [계층·저장소 ADR](../adr/0001-storage-and-layer-boundaries.md), [시간 ADR](../adr/0002-korea-time-auditing.md).

## 1. 이번 결정과 범위

사용자는 회원가입·로그인·사용자가 아직 없으므로 인증·사용자 연결은 나중에 수정하고, 지금은 반려동물 등록만 만들도록 지시했다. 기존 계획의 인증 통합 선행조건은 이번 범위에서 철회한다.

- 무인증 `POST /api/pets`를 허용하고 `caregiverId`는 null인 미연결 동물로 저장한다.
- 고정된 가짜 사용자·클라이언트 본문 사용자 ID·임의 사용자 헤더로 소유자를 만들지 않는다.
- 프로필 미디어가 없으면 정상 등록한다. non-null `profileMediaId`는 미디어 검증·연결 계약이 없으므로 저장 전 503으로 거부한다.
- 이번 등록은 MongoDB 단일 문서 원자 insert로 끝낸다. `REGISTERING` 임대·복구 상태 머신, 외부 연결 부작용, 자동 복구 작업은 만들지 않는다.
- 선택적인 `Idempotency-Key`가 이미 사용됐으면 409를 반환한다. 무인증 상태에서 이전 등록 응답을 재전달하지 않는다.

목록·상세 조회·수정·삭제, 종/품종 목록 API, 예방접종·진료, 프론트, 로그인·토큰 검증, 미디어 업로드·연결, 배포는 제외한다. `Location`은 향후 상세 리소스 URI이며 이번에 GET을 제공한다는 뜻은 아니다. 커밋·push·PR은 별도 요청 범위다.

## 2. HTTP 계약

| 항목 | 이번 동작 |
| --- | --- |
| 요청 | `POST /api/pets`, JSON, 인증 불필요 |
| 소유자 | 서버에서 null 설정. 요청 DTO에 `caregiverId` 없음 |
| 정상 응답 | `201 Created`, `Location: /api/pets/{petId}`, `{"data": PetDetail}` |
| 프로필 | 요청 생략/null 정상, non-null은 503 `MEDIA_SERVICE_UNAVAILABLE` |
| 재시도 키 | 선택적인 `Idempotency-Key`; 재사용은 payload에 관계없이 409 `IDEMPOTENCY_CONFLICT` |
| 오류 | 기존 `{"code":"...","message":"..."}` 형식 |

이는 공통 API 초안의 인증 필수·사용자별 결과 재전달 계약과 다른 **현재 개발 단계의 임시 계약**이다. 공통 요구를 확정 수정한 것으로 취급하지 않는다. 익명 키만으로 기존 동물을 조회하거나 최초 응답·ID를 돌려주지 않는다.

## 3. 입력·검증과 나이

아래는 승인된 개발 계획의 입력 정책이다. 공통 PRD의 D12가 영구 확정됐다고 기록하지 않고 서비스 구현 기준으로 유지한다.

| 필드 | Java / JSON | 필수·null | 검증·처리 |
| --- | --- | --- | --- |
| `name` | String / string | 필수·null 불가 | 앞뒤 공백 제거 후 1~50 Unicode 코드 포인트, 공백만 입력 거부 |
| `speciesId` | String / string | 필수·null 불가 | 빈 값 거부, 활성 종 기준 데이터에서 강아지·고양이 지원 검사 |
| `breedId` | String / string | 생략·null 가능 | 값이 있으면 활성 품종이며 선택 종에 속해야 함, 빈 문자열 거부 |
| `profileMediaId` | String / string | 생략·null 가능 | 빈 문자열은 400, 유효한 non-null 값은 저장 전 503 |
| `birth` | LocalDate / YYYY-MM-DD | 생략·null 가능 | 실제 날짜, 0001~9999년, 한국시간 오늘 이후 거부 |
| `sex` | String / string | 필수·null 불가 | MALE 또는 FEMALE |
| `isNeutered` | Boolean / boolean | 생략·null 가능 | true/false/null 유지, null을 false로 바꾸지 않음 |
| `weight` | BigDecimal / number | 생략·null 가능 | kg, 0 초과 200 이하, 유효 소수 최대 3자리, 반올림하지 않음 |
| `basicVaccinationCompleted` | Boolean / boolean | 생략·null 가능 | 사용자가 입력한 값, 접종 이력 자동 산정 없음 |

ID는 불투명 문자열이며 임의 소문자 변환을 하지 않는다. DTO에 없는 속성은 400으로 거부해 사용자 ID·감사 필드 주입을 막는다. 숫자·불리언 문자열을 정상 숫자·불리언으로 자동 변환하지 않는다. 이 설정은 등록 DTO에 한정한다.

나이는 저장하지 않고 최초 응답 생성 시 한국시간 오늘과 생일의 `Period.between(birth, today)`에서 만 년·잔여 개월을 구한다. 생일 null이면 age null, 오늘 태어나면 years/months는 0이다. 월말·윤년 사례를 검증한다.

## 4. MongoDB 저장 구조

반려동물·종/품종은 MongoDB에 저장한다. MySQL 접종명은 변경하지 않는다. `pets` 문서 제안은 다음과 같다.

```json
{
  "_id": "<서버 UUID 문자열>",
  "caregiverId": null,
  "name": "루이",
  "speciesId": "<종 ID>",
  "breedId": null,
  "profileMediaId": null,
  "birth": "2023-05-01",
  "sex": "MALE",
  "isNeutered": null,
  "weight": "<실제 BSON Decimal128>",
  "basicVaccinationCompleted": null,
  "registration": {
    "requestKey": "<헤더가 있을 때만 저장>",
    "responseSnapshot": "<최초 PetDetail 결과 구조>"
  },
  "createdAt": "<BSON Date>",
  "updatedAt": "<BSON Date>",
  "deletedAt": null
}
```

- 신규 문서 자체가 완료된 등록이다. 완료 응답 스냅샷과 감사 시각을 준비해 **하나의 insert에 함께 저장**한다. insert 성공이 확인된 뒤에만 201을 반환한다.
- responseSnapshot은 결과 보존용이며 익명 키 재사용자에게 반환하지 않는다. 저장 시각과 응답 시각이 일치하도록 한 생성 시각을 밀리초 단위로 절삭해 BaseDocument와 스냅샷에 설정한다. 이 insert 경로는 감사 콜백으로 시각을 재생성하지 않는다.
- 생일은 날짜 문자열로 명시 매핑해 JVM 시간대에 따라 날짜가 이동하는 것을 피한다. 도메인·HTTP에서는 LocalDate를 유지한다.
- 체중은 BigDecimal과 BSON Decimal128 사이에 명시 매핑한다. 예시 문자열은 타입 설명이며 실제 문자열 저장을 뜻하지 않는다.
- Mongo 문서는 BaseDocument를 사용하고 도메인은 상속하지 않는다. `caregiverId`, 키·스냅샷 등 내부 값은 PetDetail에 노출하지 않는다.
- 키가 없으면 requestKey를 저장하지 않는다. 모든 무키 요청이 null unique 충돌을 일으키지 않도록 partial 인덱스를 사용한다.

종 기준 `pet_species`는 `_id`, `code`(DOG/CAT), `name`, `active`, `breeds: [{breedId,name,active}]`로 구현했다. 명시적 prepare 스크립트가 기본 dog/cat 종을 없을 때만 삽입하고 기존 품종 배열을 보존한다. 초기 품종 배열은 비어 있으며 품종 사용 전 검토된 데이터를 준비한다. 종 문서 내부 품종 ID 중복도 적용 시 검사한다.

| 컬렉션 | 인덱스 | 목적 |
| --- | --- | --- |
| pets | 기본 `_id` unique | 동물 ID |
| pets | `registration.requestKey` unique, 문자열 필드가 있는 문서만 partial | 익명 등록 API 전체에서 키 중복 삽입 방지 |
| pet_species | code unique | 종 기준 코드 중복 방지 |

자동 인덱스 생성은 켜지 않는다. `scripts/mongodb/prepare-registration.js`·`verify-registration.js`로 적용·확인하며 대상 DB·기존 중복 확인 후 명시적으로 적용한다. 키 unique 인덱스가 없으면 키 포함 요청의 중복 방지를 보장할 수 없으므로 필수 인덱스 확인을 등록 준비 절차에 둔다. 동물을 지우는 TTL 인덱스는 만들지 않는다.

## 5. 코드 구조와 책임

루트 `com.louiekury.pet` 아래 `registration` 기능 패키지를 사용한다. 일반 class·비공개 생성/빌더·정적 팩토리와 기존 공통 응답·오류·시간 변환을 따른다. Java record는 사용하지 않는다.

| 패키지·파일 | 책임 |
| --- | --- |
| registration/domain/Pet.java, PetSpecies.java | 입력·생성 규칙, 기술 독립 기본정보 |
| registration/application/port/in/RegisterPetUseCase.java | 등록 입력 포트 |
| registration/application/model/RegisterPetCommand.java, PetResult.java | HTTP/Mongo에 독립적인 입력·결과 |
| registration/application/service/RegisterPetService.java | 검증·기준 조회·응답 생성·저장 조정 |
| registration/application/port/out/LoadPetSpeciesPort.java | 종/품종 조회 |
| registration/application/port/out/InsertPetPort.java | 원자 등록 insert, 중복·저장 오류 구분 |
| registration/adapter/in/web/PetRegistrationController.java | DTO·키를 Command로 변환, 201·Location·응답 생성 |
| registration/adapter/in/web/dto/request/RegisterPetRequestDto.java | 웹 타입·입력 제약 |
| registration/adapter/in/web/dto/response/PetDetailResponseDto.java | PetDetail 변환·한국시간 시각 |
| registration/adapter/out/persistence/ | Pet/Species Mongo 문서·팩토리 매핑·MongoTemplate 어댑터 |
| shared/exception/ErrorCode.java, global/web/exception/ErrorCodeHttpMapper.java | 등록 오류·HTTP 매핑 |

인증 어댑터, 외부 미디어 클라이언트·허구 내부 HTTP API, 임대·복구 서비스는 이번에 만들지 않는다. 미디어 non-null 거부는 저장 전에 유스케이스에서도 보장해 다른 입력 어댑터가 우회하지 못하게 한다.

## 6. 처리 순서와 익명 재시도

1. JSON·키를 파싱하고 입력 정책을 검증한다. 사용자 정보를 받거나 토큰 검증을 요구하지 않는다.
2. profileMediaId가 non-null이면 503으로 중단한다. 파일 검증·연결 호출이나 동물 저장은 수행하지 않는다.
3. 종·품종 기준 데이터를 조회해 활성·지원 종·소속 관계를 확인한다.
4. 검증 중 생성한 UUID와 한국시간 Clock 기반 밀리초 시각으로 caregiverId null의 PetDetail 결과를 만든다.
5. 키가 있으면 필수 partial unique 인덱스를 확인한다. 선택 키와 완료 응답 스냅샷을 포함한 문서를 원자 insert한다. 저장 어댑터가 requestKey unique 충돌만 409로 분류하고, 다른 unique 충돌·DB 장애와 구분한다.
6. 저장 성공 확인 후 최초 PetDetail을 201과 Location으로 반환한다. 저장 실패를 정상 응답으로 바꾸지 않는다.

키가 없으면 각 요청은 새 등록이다. 응답을 잃고 무키 재시도하면 중복될 수 있다. 키를 제공하면 전역 scope에서 동일 키는 payload가 같거나 달라도 409이며 기존 동물 ID·입력·응답을 공개하지 않는다. 따라서 기존 공통 초안의 동일 입력 응답 재전달·payload 비교는 이번 임시 계약에 포함하지 않는다.

키 형식은 ASCII 영숫자·`-`·`_`·`.`·`:`의 1~128자, 제공된 빈 값은 400으로 한다. 사용자는 충돌을 피하도록 충분히 무작위인 UUID를 생성한다. 동물 문서 수명 동안 키를 유지하며 만료·자동 재사용은 제공하지 않는다. 사전 존재 검사만으로 중복을 막지 않고 unique insert를 최종 보장으로 사용한다.

Mongo 단일 문서 원자성이므로 진행 중 문서·임대·202 응답은 없다. 연결 타임아웃으로 저장 결과가 불명확할 수 있으며 같은 키 재시도의 409는 이미 키가 사용됐다는 뜻만 제공한다. 익명 요청자에게 기존 결과를 확인시켜 줄 방법은 이번 범위에 없다.

## 7. 오류와 후속 통합

| 상황 | 결과 |
| --- | --- |
| JSON·타입·필수·범위·키 형식 오류 | 400 VALIDATION_ERROR, 저장 안 함 |
| 종/품종 없음·비활성·소속 불일치 | 400 VALIDATION_ERROR, 저장 안 함 |
| profileMediaId non-null | 503 MEDIA_SERVICE_UNAVAILABLE, 저장 안 함 |
| 키 재사용·동시 동일 키 | 409 IDEMPOTENCY_CONFLICT, 기존 응답 노출 안 함 |
| 종/품종 조회 장애 | 503 REFERENCE_DATA_UNAVAILABLE, 없는 종으로 취급 안 함 |
| Mongo 저장 장애 | 503 PERSISTENCE_UNAVAILABLE, 성공 응답 안 함 |
| 예상하지 못한 오류 | 500 INTERNAL_SERVER_ERROR, 내부 원문 노출 안 함 |

인증 서비스가 생기면 검증된 인증 주체, 요청별 소유권, 인증 누락·권한 오류와 사용자별 멱등성 scope를 별도 구현한다. 기존 caregiverId null 데이터의 귀속은 자동으로 최초 로그인 사용자에게 부여하지 않는다. 신뢰할 수 있는 연결 증명·별도 이관 정책을 정해야 하며 이번에는 미연결 상태로 남긴다.

미디어 GET 응답만으로는 업로더·원자 연결·부분 실패 복구를 확인할 수 없다. 실제 검증·연결 계약을 합의한 뒤 프로필 지원을 추가한다. 그때 외부 작업의 중복 안전성·연결 확인/취소·Mongo와의 부분 실패 복구를 새로 설계한다.

## 8. 개발·검증 순서와 완료 기준

[개발 이슈 #5](https://github.com/louie-kury/louie-kury-pet-service/issues/5)에 대해 사용자가 구현을 승인했다. 아래 순서로 구현·검증을 진행했고 결과를 마지막에 기록했다.

| 단계 | 산출물·통과 기준 |
| --- | --- |
| 1. 내부 구현 | 도메인·유스케이스·포트·Mongo 매퍼/어댑터, 입력·기준·null 소유자 규칙 검사 |
| 2. 웹 구현 | 무인증 등록·201·Location·응답·오류, 사용자 ID 주입 거부 검사 |
| 3. Mongo 준비 | 인덱스 적용/확인·승인된 종 기준 데이터 절차, 단일 insert와 키 충돌 검사 |
| 4. 검증·리뷰 | 관련 테스트·빌드·독립 코드 리뷰·가능한 실행 API QA, 실제/대체 환경 구분 |
| 5. 안내 정리 | README에 무인증·미연결·미디어 제한·임시 재시도 계약·실행 결과 반영 |

구현은 implementer, 테스트는 tester, 코드 리뷰와 API QA는 별도 reviewer에게 맡긴다. 재작업은 기존 최초 검증 후 최대 두 차례 규칙을 따른다.

검증할 핵심 사례는 최소/전체 기본정보, 각 선택 필드 null, 이름/날짜/체중 경계, 타입·알 수 없는 필드 오류, 다른 종의 품종, 기준 데이터 장애다. 미디어 non-null은 저장이 전혀 일어나지 않는지 확인한다.

무인증 정상 201, caregiverId null 저장, 고정 사용자 부재, 본문 caregiverId 거부, 임의 사용자 헤더로 소유자 변경 불가를 확인한다. 키 없음의 독립 등록, 동일 키의 같은/다른 payload 409와 기존 결과 비노출, 동시 동일 키의 정확히 한 건 저장을 구분해 확인한다.

실제 Mongo에서는 UUID·Decimal128·날짜 문자열·감사 시각 왕복, JVM UTC/Asia/Seoul에서 생일 동일, partial unique 인덱스와 무키 다건 insert를 검사한다. 테스트 DB·전용 컬렉션만 사용하고 기존 개발 데이터를 지우지 않는다. 완료 스냅샷과 저장 감사 시각의 일치도 확인한다.

JDK 17에서 관련 검사 후 `./gradlew test`·`./gradlew build`를 실행한다. Mock·웹 테스트는 실제 Mongo·실행 서버 검증과 구분하고 인증·미디어는 이번 미구현 범위로 보고한다. 같은 상태 검사를 완료 보고용으로 반복하지 않는다.

2026-11-23 JDK 17에서 `PET_TEST_MONGODB_URI=mongodb://127.0.0.1:27028 ./gradlew test build`가 성공했다. 전체 101개·실패 0·건너뜀 0이며 실제 MongoDB 검사 11개를 포함한다. JSON 소수 원문 해석 두 사례와 응답/저장 시각 정밀도 불일치 사례의 수정 회귀도 통과했다. 독립 코드 리뷰의 남은 지적은 없다.

로컬 MongoDB 8.0·MySQL 8.4.13에 연결한 표준 프로필 실행 JAR 기동과 QA DB의 prepare/verify 스크립트 적용을 확인했다. 실제 API 독립 QA도 통과했다. 위 주소·포트는 검증 당시 환경이며 고정 실행 지침이 아니다. Atlas·인증·미디어 서비스 연동은 이 결과에 포함하지 않는다.

독립 QA는 실행 서버에서 39개 시나리오·42개 요청과 최초 정상 등록 1건을 확인했다. 정상 저장 10건의 명시적 caregiverId null·생일 문자열·Decimal128·스냅샷/감사 시각 일치, 같은 키 동시 4건의 201 한 건·409 세 건 및 실패 시 미저장을 확인했다. 알 수 없는 속성·타입·사용자 ID 주입·종/품종·미디어 제한·미세 소수 경계도 확인했으며 남은 결함은 없다. 재작업은 1회 사용했고 잔여 1회다.
