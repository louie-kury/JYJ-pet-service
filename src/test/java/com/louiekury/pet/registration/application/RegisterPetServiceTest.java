package com.louiekury.pet.registration.application;

import com.louiekury.pet.registration.application.model.PetResult;
import com.louiekury.pet.registration.application.model.RegisterPetCommand;
import com.louiekury.pet.registration.application.port.out.InsertPetPort;
import com.louiekury.pet.registration.application.port.out.LoadPetSpeciesPort;
import com.louiekury.pet.registration.application.service.RegisterPetService;
import com.louiekury.pet.registration.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
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

class RegisterPetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:30:00Z");
    private LoadPetSpeciesPort species;
    private InsertPetPort insert;
    private RegisterPetService service;

    @BeforeEach
    void setUp() {
        species = mock(LoadPetSpeciesPort.class);
        insert = mock(InsertPetPort.class);
        service = new RegisterPetService(species, insert, Clock.fixed(NOW, ZoneOffset.UTC));
        when(species.findById("dog")).thenReturn(Optional.of(
            new PetSpecies("dog", "강아지", "DOG", true, Map.of("poodle", "푸들"))));
    }

    @Test
    @DisplayName("인증 주체 없이 최소 정보로 등록하면 새 결과를 저장한다")
    void registersWithoutMemberOrOptionalValues() {
        PetResult result = service.register(command(null, null, null, null));
        assertThat(result.getName()).isEqualTo("루이");
        assertThat(result.getSpecies().getSpeciesId()).isEqualTo("dog");
        assertThat(result.getSpecies().getName()).isEqualTo("강아지");
        assertThat(result.getBreed()).isNull();
        assertThat(result.getAge()).isNull();
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        assertThat(result.getUpdatedAt()).isEqualTo(NOW);
        verify(insert).insert(result, null);
    }

    @Test
    @DisplayName("전체 기본정보와 품종을 등록하면 입력값과 기준 명칭이 결과에 반영된다")
    void returnsEnteredFieldsAndResolvedBreed() {
        PetResult result = service.register(RegisterPetCommand.of("루이", "dog", "poodle", null,
            LocalDate.of(2023, 5, 1), "FEMALE", false, new BigDecimal("5.123"), true, "request-1"));
        assertThat(result.getBreed().getName()).isEqualTo("푸들");
        assertThat(result.getSex()).isEqualTo("FEMALE");
        assertThat(result.getIsNeutered()).isFalse();
        assertThat(result.getWeight()).isEqualByComparingTo("5.123");
        assertThat(result.getBasicVaccinationCompleted()).isTrue();
        assertThat(result.getAge().getYears()).isEqualTo(3);
        assertThat(result.getAge().getMonths()).isEqualTo(6);
        verify(insert).insert(result, "request-1");
    }

    @Test
    @DisplayName("UTC 날짜가 전날이어도 한국시간 오늘 태어난 동물을 등록할 수 있다")
    void usesKoreanDateRegardlessOfClockZone() {
        PetResult result = service.register(command(null, null, LocalDate.of(2026, 11, 23), null));
        assertThat(result.getAge().getYears()).isZero();
        assertThat(result.getAge().getMonths()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"2024-02-29,2,8", "2026-10-31,0,0"})
    @DisplayName("윤년과 월말 생일도 정해진 만 년·개월 계산을 따른다")
    void calculatesAgeAcrossLeapAndMonthEnd(String birth, int years, int months) {
        PetResult result = service.register(command(null, null, LocalDate.parse(birth), null));
        assertThat(result.getAge().getYears()).isEqualTo(years);
        assertThat(result.getAge().getMonths()).isEqualTo(months);
    }

    @Test
    @DisplayName("다른 종의 품종을 보내면 저장하지 않는다")
    void rejectsBreedOutsideSelectedSpecies() {
        assertCode(() -> service.register(command("other-breed", null, null, null)), ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("없는 종은 등록하지 않는다")
    void rejectsMissingSpecies() {
        when(species.findById("dog")).thenReturn(Optional.empty());
        assertCode(() -> service.register(command(null, null, null, null)), ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(insert);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOG-INACTIVE", "RABBIT"})
    @DisplayName("비활성 종 또는 지원하지 않는 종은 등록하지 않는다")
    void rejectsUnavailableSpecies(String kind) {
        when(species.findById("dog")).thenReturn(Optional.of(new PetSpecies("dog", "동물",
            kind.equals("DOG-INACTIVE") ? "DOG" : "RABBIT", !kind.equals("DOG-INACTIVE"), Map.of())));
        assertCode(() -> service.register(command(null, null, null, null)), ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("미디어 연동 전에는 프로필 ID가 있는 등록을 저장 전에 거부한다")
    void refusesMediaWithoutPretendingItIsVerified() {
        assertCode(() -> service.register(command(null, "media-1", null, null)), ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("키가 없는 독립 요청은 각각 새 동물로 등록한다")
    void createsIndependentPetsWithoutKey() {
        PetResult first = service.register(command(null, null, null, null));
        PetResult second = service.register(command(null, null, null, null));
        assertThat(first.getPetId()).isNotEqualTo(second.getPetId());
        verify(insert, times(2)).insert(any(PetResult.class), isNull());
    }

    @Test
    @DisplayName("저장 실패는 성공 결과로 변환하지 않는다")
    void propagatesStorageFailure() {
        doThrow(new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE)).when(insert).insert(any(), isNull());
        assertCode(() -> service.register(command(null, null, null, null)), ErrorCode.PERSISTENCE_UNAVAILABLE);
    }

    private static RegisterPetCommand command(String breed, String media, LocalDate birth, String key) {
        return RegisterPetCommand.of("루이", "dog", breed, media, birth, "MALE", null, null, null, key);
    }

    private static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
