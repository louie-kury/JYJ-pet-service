package com.louiekury.pet.pet.application;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.GetPetsResult;
import com.louiekury.pet.pet.application.getpets.GetPetsService;
import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.pet.application.getpets.PetListPage;
import com.louiekury.pet.pet.application.port.out.GetPetsPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetPetsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");
    private GetPetsPort pets;
    private GetPetsService service;

    @BeforeEach
    void setUp() {
        pets = mock(GetPetsPort.class);
        service = new GetPetsService(pets, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsMissingOwnerWithoutQueryingPets() {
        assertCode(() -> service.getPets(GetPetsQuery.of(0, 4, null)), ErrorCode.UNAUTHORIZED);
        assertCode(() -> service.getPets(GetPetsQuery.of(0, 4, " ")), ErrorCode.UNAUTHORIZED);
        org.mockito.Mockito.verifyNoInteractions(pets);
    }

    @Test
    @DisplayName("여러 동물의 현재 이름·종·품종·성별을 서로 섞지 않고 반환한다")
    void preservesEachPetsSummaryFields() {
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(
            PetListData.of("pet-dog", "루이", new PetSpecies("dog", "강아지"), "poodle", "푸들", null,
                LocalDate.of(2023, 5, 1), "MALE"),
            PetListData.of("pet-cat", "쿠리", new PetSpecies("cat", "고양이"), null, null, null, null, "FEMALE")), 2));

        GetPetsResult result = service.getPets(GetPetsQuery.of(0, 4, "owner-a"));

        assertThat(result.getContent()).hasSize(2);
        var dog = result.getContent().get(0);
        assertThat(dog.getPetId()).isEqualTo("pet-dog");
        assertThat(dog.getName()).isEqualTo("루이");
        assertThat(dog.getSpecies().getSpeciesId()).isEqualTo("dog");
        assertThat(dog.getSpecies().getName()).isEqualTo("강아지");
        assertThat(dog.getBreed().getBreedId()).isEqualTo("poodle");
        assertThat(dog.getBreed().getName()).isEqualTo("푸들");
        assertThat(dog.getSex()).isEqualTo("MALE");
        assertThat(dog.getAge().getYears()).isEqualTo(3);
        assertThat(dog.getAge().getMonths()).isEqualTo(6);
        var cat = result.getContent().get(1);
        assertThat(cat.getPetId()).isEqualTo("pet-cat");
        assertThat(cat.getName()).isEqualTo("쿠리");
        assertThat(cat.getSpecies().getSpeciesId()).isEqualTo("cat");
        assertThat(cat.getSpecies().getName()).isEqualTo("고양이");
        assertThat(cat.getBreed()).isNull();
        assertThat(cat.getAge()).isNull();
        assertThat(cat.getSex()).isEqualTo("FEMALE");
    }

    @ParameterizedTest
    @CsvSource({
        "2026-11-23,0,0", "2025-11-23,1,0", "2025-11-24,0,11",
        "2024-02-29,2,8", "2026-10-31,0,0", "2026-10-23,0,1"
    })
    @DisplayName("한국날짜 기준 생일·생일 전날·윤년·월말 나이를 만 년과 잔여 개월로 계산한다")
    void calculatesAgeFromKoreanDate(String birth, int years, int months) {
        when(pets.getPets(any())).thenReturn(PetListPage.of(
            List.of(pet(LocalDate.parse(birth), null)), 1));

        var age = service.getPets(GetPetsQuery.of(0, 4, "owner-a")).getContent().get(0).getAge();

        assertThat(age.getYears()).isEqualTo(years);
        assertThat(age.getMonths()).isEqualTo(months);
    }

    @ParameterizedTest
    @CsvSource({"0,0,0,false", "4,0,1,false", "5,0,2,true", "8,1,2,false", "9,1,3,true", "9,2,3,false", "9,3,3,false"})
    @DisplayName("빈 결과·첫 페이지·마지막 페이지·범위 밖 페이지의 페이지 정보가 일치한다")
    void returnsCorrectPageMetadata(long total, int page, long totalPages, boolean hasNext) {
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(), total));

        GetPetsResult result = service.getPets(GetPetsQuery.of(page, 4, "owner-a"));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPage()).isEqualTo(page);
        assertThat(result.getSize()).isEqualTo(4);
        assertThat(result.getTotalElements()).isEqualTo(total);
        assertThat(result.getTotalPages()).isEqualTo(totalPages);
        assertThat(result.isHasNext()).isEqualTo(hasNext);
    }

    @ParameterizedTest
    @CsvSource({"-1,4", "0,0", "0,-1", "0,101"})
    @DisplayName("잘못된 페이지 번호·조회 개수는 DB 조회 전에 검증 오류로 거부한다")
    void rejectsInvalidPageConditions(int page, int size) {
        assertCode(() -> service.getPets(GetPetsQuery.of(page, size, "owner-a")), ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(pets);
    }

    @Test
    @DisplayName("최대 조회 개수 100과 큰 페이지 번호의 offset은 int 오버플로 없이 전달한다")
    void supportsMaximumSizeAndLargeOffset() {
        when(pets.getPets(any())).thenAnswer(invocation -> {
            GetPetsQuery query = invocation.getArgument(0);
            assertThat(query.getOffset()).isEqualTo(214748364700L);
            return PetListPage.of(List.of(), 0);
        });

        GetPetsResult result = service.getPets(GetPetsQuery.of(Integer.MAX_VALUE, 100, "owner-a"));

        assertThat(result.getPage()).isEqualTo(Integer.MAX_VALUE);
        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("큰 전체 개수의 전체 페이지 수는 long 오버플로 없이 계산한다")
    void calculatesLargeTotalWithoutOverflow() {
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(), Long.MAX_VALUE));

        GetPetsResult result = service.getPets(GetPetsQuery.of(0, 4, "owner-a"));

        assertThat(result.getTotalPages()).isEqualTo(2305843009213693952L);
        assertThat(result.isHasNext()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"PET_QUERY_UNAVAILABLE", "REFERENCE_DATA_UNAVAILABLE"})
    @DisplayName("조회·기준 데이터 장애는 정상 빈 목록으로 숨기지 않는다")
    void preservesPortFailures(ErrorCode code) {
        when(pets.getPets(any())).thenThrow(new BusinessException(code));
        assertCode(() -> service.getPets(GetPetsQuery.of(0, 4, "owner-a")), code);
    }

    @Test
    @DisplayName("프로필 파일이 있으나 연동되지 않았다면 정상 파일처럼 응답하지 않는다")
    void rejectsUnresolvedMedia() {
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(pet(null, "media-1")), 1));
        assertCode(() -> service.getPets(GetPetsQuery.of(0, 4, "owner-a")), ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("저장된 미래 생일은 음수 나이를 반환하지 않고 조회 오류로 처리한다")
    void rejectsCorruptedFutureBirth() {
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(pet(LocalDate.of(2026, 11, 24), null)), 1));
        assertCode(() -> service.getPets(GetPetsQuery.of(0, 4, "owner-a")), ErrorCode.PET_QUERY_UNAVAILABLE);
    }

    private static PetListData pet(LocalDate birth, String mediaId) {
        return PetListData.of("pet-1", "루이", new PetSpecies("dog", "강아지"), null, null, mediaId, birth, "MALE");
    }

    private static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
