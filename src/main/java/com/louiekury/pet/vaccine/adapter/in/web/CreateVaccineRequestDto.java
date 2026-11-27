package com.louiekury.pet.vaccine.adapter.in.web;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateVaccineRequestDto {
    private String name;

    public void setName(Object name) {
        this.name = name instanceof String ? (String) name : null;
    }
}
