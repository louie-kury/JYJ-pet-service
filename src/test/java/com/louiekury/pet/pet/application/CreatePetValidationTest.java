package com.louiekury.pet.pet.application;

import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.createpet.CreatePetService;
import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.mockito.Mockito.mock;
import com.louiekury.pet.pet.application.port.out.CreatePetPort;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePetValidationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    @DisplayName("최소 필수 정보로 등록하면 이름을 정리하고 선택값은 미상으로 보존한다")
    void minimumInputPreservesUnknownValues() {
        PetResult pet = create("  루이  ", null, null);
        assertThat(pet.getName()).isEqualTo("루이");
        assertThat(pet.getSpecies().getSpeciesId()).isEqualTo("dog");
        assertThat(pet.getPetId()).isNotBlank();
        assertThat(pet.getBirth()).isNull();
        assertThat(pet.getWeight()).isNull();
        assertThat(pet.getIsNeutered()).isNull();
        assertThat(pet.getBasicVaccinationCompleted()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("이름이 없거나 공백뿐이면 등록을 거부한다")
    void rejectsMissingNames(String name) {
        assertValidation(() -> create(name, null, null));
    }

    @Test
    @DisplayName("이름은 유니코드 코드 포인트 50자까지 허용한다")
    void acceptsFiftyUnicodeCodePointsAndRejectsFiftyOne() {
        assertThat(create("🐶".repeat(50), null, null).getName()).isEqualTo("🐶".repeat(50));
        assertValidation(() -> create("🐶".repeat(51), null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.001", "200.001", "0.0001", "12.1234"})
    @DisplayName("체중이 양수 범위 또는 소수 정밀도를 벗어나면 거부한다")
    void rejectsInvalidWeight(String weight) {
        assertValidation(() -> create("루이", null, new BigDecimal(weight)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.001", "200", "200.0000", "12.123", "1E+2"})
    @DisplayName("체중의 경계값과 의미 없는 후행 0은 허용하고 값을 반올림하지 않는다")
    void acceptsWeightBoundaries(String weight) {
        assertThat(create("루이", null, new BigDecimal(weight)).getWeight())
            .isEqualByComparingTo(weight);
    }

    @Test
    @DisplayName("오늘 생일은 허용하고 미래 또는 지원 연도 밖의 생일은 거부한다")
    void checksBirthBoundaries() {
        assertThat(create("루이", TODAY, null).getBirth()).isEqualTo(TODAY);
        assertThat(create("루이", LocalDate.of(1, 1, 1), null).getBirth()).isEqualTo(LocalDate.of(1, 1, 1));
        assertValidation(() -> create("루이", TODAY.plusDays(1), null));
        assertValidation(() -> create("루이", LocalDate.of(0, 1, 1), null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN", "male"})
    @DisplayName("성별은 지정된 외부 값만 허용한다")
    void rejectsInvalidSex(String sex) {
        assertValidation(() -> createWith("루이", null, null, null, sex, null));
    }

    @Test
    @DisplayName("선택한 식별자의 빈 문자열은 미상으로 간주하지 않는다")
    void rejectsBlankOptionalIdentifiers() {
        assertValidation(() -> createWith("루이", " ", null, null, "MALE", null));
        assertValidation(() -> createWith("루이", null, "", null, "MALE", null));
    }

    private static PetResult create(String name, LocalDate birth, BigDecimal weight) {
        return createWith(name, null, null, birth, "MALE", weight);
    }

    private static PetResult createWith(String name, String breed, String media, LocalDate birth,
            String sex, BigDecimal weight) {
        CreatePetService service = new CreatePetService(mock(GetPetBreedPort.class), com.louiekury.pet.pet.support.CatalogFixtures.species(), mock(CreatePetPort.class),
            Clock.fixed(Instant.parse("2026-11-23T00:00:00Z"), ZoneOffset.UTC));
        return service.createPet(CreatePetCommand.of(name, "dog", breed, media, birth, sex, null,
            weight, null, null, "member-1"));
    }

    private static void assertValidation(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
