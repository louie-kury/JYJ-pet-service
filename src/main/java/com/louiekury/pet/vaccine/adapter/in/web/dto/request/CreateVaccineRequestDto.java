package com.louiekury.pet.vaccine.adapter.in.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateVaccineRequestDto {
    private String name;

    public void setName(String name) {
        this.name = name;
    }
}
