package com.louiekury.pet.pet.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface UpdatePetPort {
    @Getter
    @RequiredArgsConstructor
    class Data {
        private final String id;
        private final String name;
        private final String speciesId;
        private final String breedId;
        private final String breedName;
        private final String profileMediaId;
        private final LocalDate birth;
        private final String sex;
        private final Boolean neutered;
        private final BigDecimal weight;
        private final Boolean vaccination;
        private final Instant createdAt;
        private final Instant updatedAt;
    }

    Optional<Data> find(String id, Map<String, Object> changes, String caregiverId);
    boolean update(Data previous, Map<String, Object> changes, Instant updatedAt, String caregiverId);
}
