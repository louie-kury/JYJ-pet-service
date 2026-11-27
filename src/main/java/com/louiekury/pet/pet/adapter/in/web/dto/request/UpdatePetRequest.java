package com.louiekury.pet.pet.adapter.in.web.dto.request;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UpdatePetRequest {
    private final java.util.Map<String, Object> changes = new java.util.LinkedHashMap<>();

    public void setName(Object value) {
        put("name", RequestValue.text(value));
    }

    public void setSpeciesId(Object value) {
        put("speciesId", RequestValue.text(value));
    }

    public void setBreedId(Object value) {
        put("breedId", RequestValue.text(value));
    }

    public void setProfileMediaId(Object value) {
        put("profileMediaId", RequestValue.text(value));
    }

    public void setBirth(Object value) {
        put("birth", RequestValue.date(value));
    }

    public void setSex(Object value) {
        put("sex", RequestValue.text(value));
    }

    public void setIsNeutered(Object value) {
        put("isNeutered", RequestValue.bool(value));
    }

    public void setWeight(Object value) {
        put("weight", RequestValue.decimal(value));
    }

    public void setBasicVaccinationCompleted(Object value) {
        put("basicVaccinationCompleted", RequestValue.bool(value));
    }

    public java.util.Map<String, Object> toChanges() {
        return changes;
    }

    private void put(String field, Object value) {
        if (changes.containsKey(field)) {
            throw RequestValue.invalid();
        }
        changes.put(field, value);
    }
}
