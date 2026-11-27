package com.louiekury.pet.pet.application.port.in;

import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.createpet.PetResult;

public interface CreatePetUseCase {

    PetResult createPet(CreatePetCommand command);
}
