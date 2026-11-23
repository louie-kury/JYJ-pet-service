package com.louiekury.pet.registration.domain;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PetSpeciesTest {

    @Test
    @DisplayName("지원 종은 강아지와 고양이 두 enum 값뿐이다")
    void hasOnlySupportedSpecies() {
        assertThat(PetSpecies.values()).containsExactly(PetSpecies.DOG, PetSpecies.CAT);
    }

    @ParameterizedTest
    @CsvSource({"dog,DOG,강아지", "cat,CAT,고양이"})
    @DisplayName("외부 ID를 실제 enum으로 변환하고 표시 이름을 enum에서 제공한다")
    void resolvesCanonicalIdentifiers(String id, PetSpecies expected, String name) {
        PetSpecies species = PetSpecies.fromId(id);
        assertThat(species).isSameAs(expected);
        assertThat(species.getSpeciesId()).isEqualTo(id);
        assertThat(species.getName()).isEqualTo(name);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"DOG", "CAT", "Dog", " dog", "cat ", "rabbit", " ", "507f1f77bcf86cd799439011"})
    @DisplayName("미지정·미지원 종과 대문자 별칭은 자동 보정하지 않고 거부한다")
    void rejectsNonCanonicalIdentifiers(String id) {
        assertThatThrownBy(() -> PetSpecies.fromId(id)).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
