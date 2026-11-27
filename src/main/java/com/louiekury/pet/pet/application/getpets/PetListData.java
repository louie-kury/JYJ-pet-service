package com.louiekury.pet.pet.application.getpets;

import com.louiekury.pet.pet.domain.PetSpecies;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PetListData {

    private final String petId;
    private final String name;
    private final PetSpecies species;
    private final String breedId;
    private final String breedName;
    private final String profileMediaId;
    private final LocalDate birth;
    private final String sex;

    public static PetListData of(String petId, String name, PetSpecies species, String breedId,
            String breedName, String profileMediaId, LocalDate birth, String sex) {
        return PetListData.builder()
            .petId(petId)
            .name(name)
            .species(species)
            .breedId(breedId)
            .breedName(breedName)
            .profileMediaId(profileMediaId)
            .birth(birth)
            .sex(sex)
            .build();
    }
}
