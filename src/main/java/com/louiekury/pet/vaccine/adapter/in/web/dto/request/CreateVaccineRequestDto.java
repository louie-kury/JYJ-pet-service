package com.louiekury.pet.vaccine.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateVaccineRequestDto {
    @NotBlank(message = "접종명은 필수입니다.")
    private String name;
}
