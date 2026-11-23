package com.louiekury.pet.pet.application;

import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.port.out.CreatePetPort;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.application.createpet.CreatePetService;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class CreatePetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:30:00Z");
    private GetPetBreedPort breeds;
    private CreatePetPort insert;
    private CreatePetService service;

    @BeforeEach
    void setUp() {
        breeds = mock(GetPetBreedPort.class);
        insert = mock(CreatePetPort.class);
        service = new CreatePetService(breeds, insert, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("인증 주체 없이 최소 정보로 등록하면 새 결과를 저장한다")
    void registersWithoutMemberOrOptionalValues() {
        PetResult result = service.createPet(command(null, null, null, null));
        assertThat(result.getName()).isEqualTo("루이");
        assertThat(result.getSpecies().getSpeciesId()).isEqualTo("dog");
        assertThat(result.getSpecies().getName()).isEqualTo("강아지");
        assertThat(result.getBreed()).isNull();
        assertThat(result.getAge()).isNull();
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        assertThat(result.getUpdatedAt()).isEqualTo(NOW);
        verify(insert).createPet(result, null);
        verifyNoInteractions(breeds);
    }

    @Test
    @DisplayName("전체 기본정보와 품종을 등록하면 입력값과 기준 명칭이 결과에 반영된다")
    void returnsEnteredFieldsAndResolvedBreed() {
        when(breeds.getPetBreedBySpeciesAndId(PetSpecies.DOG, "poodle"))
            .thenReturn(Optional.of(new PetBreed(PetSpecies.DOG, "poodle", "푸들")));
        PetResult result = service.createPet(CreatePetCommand.of("루이", "dog", "poodle", null,
            LocalDate.of(2023, 5, 1), "FEMALE", false, new BigDecimal("5.123"), true, "request-1"));
        assertThat(result.getBreed().getName()).isEqualTo("푸들");
        assertThat(result.getSex()).isEqualTo("FEMALE");
        assertThat(result.getIsNeutered()).isFalse();
        assertThat(result.getWeight()).isEqualByComparingTo("5.123");
        assertThat(result.getBasicVaccinationCompleted()).isTrue();
        assertThat(result.getAge().getYears()).isEqualTo(3);
        assertThat(result.getAge().getMonths()).isEqualTo(6);
        verify(insert).createPet(result, "request-1");
    }

    @Test
    @DisplayName("UTC 날짜가 전날이어도 한국시간 오늘 태어난 동물을 등록할 수 있다")
    void usesKoreanDateRegardlessOfClockZone() {
        PetResult result = service.createPet(command(null, null, LocalDate.of(2026, 11, 23), null));
        assertThat(result.getAge().getYears()).isZero();
        assertThat(result.getAge().getMonths()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"2024-02-29,2,8", "2026-10-31,0,0"})
    @DisplayName("윤년과 월말 생일도 정해진 만 년·개월 계산을 따른다")
    void calculatesAgeAcrossLeapAndMonthEnd(String birth, int years, int months) {
        PetResult result = service.createPet(command(null, null, LocalDate.parse(birth), null));
        assertThat(result.getAge().getYears()).isEqualTo(years);
        assertThat(result.getAge().getMonths()).isEqualTo(months);
    }

    @Test
    @DisplayName("다른 종의 품종을 보내면 저장하지 않는다")
    void rejectsBreedOutsideSelectedSpecies() {
        assertCode(() -> service.createPet(command("other-breed", null, null, null)), ErrorCode.VALIDATION_ERROR);
        verify(breeds).getPetBreedBySpeciesAndId(PetSpecies.DOG, "other-breed");
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("품종을 지정하지 않으면 기준 데이터 조회 장애와 무관하게 enum 종으로 등록한다")
    void registersWithoutConsultingBreedDatabase() {
        when(breeds.getPetBreedBySpeciesAndId(any(), anyString()))
            .thenThrow(new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        PetResult result = service.createPet(command(null, null, null, null));
        assertThat(result.getSpecies().getSpeciesId()).isEqualTo("dog");
        assertThat(result.getSpecies().getName()).isEqualTo("강아지");
        verifyNoInteractions(breeds);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOG", "CAT", "rabbit"})
    @DisplayName("종 enum 외부 ID 이외의 별칭은 저장하지 않는다")
    void rejectsUnsupportedSpeciesIdentifiers(String id) {
        assertCode(() -> service.createPet(CreatePetCommand.of("루이", id, null, null, null,
            "MALE", null, null, null, null)), ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(breeds);
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("품종 조회 장애는 품종 미상 등록으로 바꾸지 않고 실패시킨다")
    void preservesBreedLookupFailure() {
        when(breeds.getPetBreedBySpeciesAndId(PetSpecies.DOG, "poodle"))
            .thenThrow(new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        assertCode(() -> service.createPet(command("poodle", null, null, null)), ErrorCode.REFERENCE_DATA_UNAVAILABLE);
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("미디어 연동 전에는 프로필 ID가 있는 등록을 저장 전에 거부한다")
    void refusesMediaWithoutPretendingItIsVerified() {
        assertCode(() -> service.createPet(command(null, "media-1", null, null)), ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("키가 없는 독립 요청은 각각 새 동물로 등록한다")
    void createsIndependentPetsWithoutKey() {
        PetResult first = service.createPet(command(null, null, null, null));
        PetResult second = service.createPet(command(null, null, null, null));
        assertThat(first.getPetId()).isNotEqualTo(second.getPetId());
        verify(insert, times(2)).createPet(any(PetResult.class), isNull());
    }

    @Test
    @DisplayName("저장 실패는 성공 결과로 변환하지 않는다")
    void propagatesStorageFailure() {
        doThrow(new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE)).when(insert).createPet(any(), isNull());
        assertCode(() -> service.createPet(command(null, null, null, null)), ErrorCode.PERSISTENCE_UNAVAILABLE);
    }

    private static CreatePetCommand command(String breed, String media, LocalDate birth, String key) {
        return CreatePetCommand.of("루이", "dog", breed, media, birth, "MALE", null, null, null, key);
    }

    private static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
