package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.application.createpet.PetResult;

public interface CreatePetPort {

    void createPet(PetResult pet, String idempotencyKey);
}
