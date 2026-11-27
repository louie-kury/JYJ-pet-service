package com.louiekury.pet.pet.adapter.in.web.dto.response;

import com.louiekury.pet.pet.application.getpets.PetListItemResult;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PetListItemResponseDto {

    private final String petId;
    private final String name;
    private final PetListItemResult.Species species;
    private final PetListItemResult.Breed breed;
    private final Object profileMedia;
    private final PetListItemResult.Age age;
    private final String sex;

    public static PetListItemResponseDto from(PetListItemResult result) {
        return PetListItemResponseDto.builder()
            .petId(result.getPetId())
            .name(result.getName())
            .species(result.getSpecies())
            .breed(result.getBreed())
            .profileMedia(null)
            .age(result.getAge())
            .sex(result.getSex())
            .build();
    }
}
