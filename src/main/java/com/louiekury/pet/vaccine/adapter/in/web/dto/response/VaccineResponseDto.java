package com.louiekury.pet.vaccine.adapter.in.web.dto.response;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class VaccineResponseDto {
    private final String vaccineNameId;
    private final String name;

    public static VaccineResponseDto from(VaccineResult result) {
        return VaccineResponseDto.builder()
            .vaccineNameId(result.getVaccineNameId())
            .name(result.getName())
            .build();
    }
}
