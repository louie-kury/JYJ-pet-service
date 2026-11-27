package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.application.getpets.PetListData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface GetPetPort {
    record Data(PetListData summary, Boolean neutered, BigDecimal weight, Boolean vaccination,
                Instant createdAt, Instant updatedAt) { }

    Optional<Data> getPet(String id, String caregiverId);
}
