package com.louiekury.pet.vaccine.adapter.in.web;

import lombok.Getter;
import tools.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
public class CreateVaccineRequestDto {
    private final String name;

    @JsonCreator
    public CreateVaccineRequestDto(@JsonProperty("name") JsonNode name) {
        this.name = name != null && name.isString() ? name.asString() : null;
    }
}
