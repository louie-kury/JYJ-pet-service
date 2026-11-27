package com.louiekury.pet.pet.application.port.in;

import com.louiekury.pet.pet.application.createpet.PetResult;

public interface GetPetUseCase {
    PetResult getPet(String id, String caregiverId);
}
