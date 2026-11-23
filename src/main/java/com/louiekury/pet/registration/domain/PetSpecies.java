package com.louiekury.pet.registration.domain;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PetSpecies {

    DOG("dog", "강아지"),
    CAT("cat", "고양이");

    private final String speciesId;
    private final String name;

    public static PetSpecies fromId(String speciesId) {
        for (PetSpecies species : values()) {
            if (species.speciesId.equals(speciesId)) {
                return species;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
