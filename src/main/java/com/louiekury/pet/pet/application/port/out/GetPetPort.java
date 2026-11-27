package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.application.getpets.PetListData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface GetPetPort {
    @Getter
    @RequiredArgsConstructor
    class Data {
        private final PetListData summary;
        private final Boolean neutered;
        private final BigDecimal weight;
        private final Boolean vaccination;
        private final Instant createdAt;
        private final Instant updatedAt;
    }

    Optional<Data> getPet(String id, String caregiverId);
}
