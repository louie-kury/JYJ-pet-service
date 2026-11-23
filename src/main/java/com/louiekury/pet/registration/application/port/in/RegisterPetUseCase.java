package com.louiekury.pet.registration.application.port.in;

import com.louiekury.pet.registration.application.model.PetResult;
import com.louiekury.pet.registration.application.model.RegisterPetCommand;

public interface RegisterPetUseCase {

    PetResult register(RegisterPetCommand command);
}
