package com.louiekury.pet.pet.application.createpet;

import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.createpet.CreatePetCommand;

public interface CreatePetUseCase {

    PetResult createPet(CreatePetCommand command);
}
