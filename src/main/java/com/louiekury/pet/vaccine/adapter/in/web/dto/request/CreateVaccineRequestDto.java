package com.louiekury.pet.vaccine.adapter.in.web.dto.request;

import java.beans.ConstructorProperties;
import lombok.Getter;

@Getter
public class CreateVaccineRequestDto {
    private final String name;

    @ConstructorProperties("name")
    public CreateVaccineRequestDto(String name) {
        this.name = name;
    }
}
