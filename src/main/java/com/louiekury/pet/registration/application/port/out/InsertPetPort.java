package com.louiekury.pet.registration.application.port.out;

import com.louiekury.pet.registration.application.model.PetResult;

public interface InsertPetPort {

    void insert(PetResult pet, String idempotencyKey);
}
