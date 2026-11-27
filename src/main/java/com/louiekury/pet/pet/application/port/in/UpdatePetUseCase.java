package com.louiekury.pet.pet.application.port.in;

import com.louiekury.pet.pet.application.createpet.PetResult;
import java.util.Map;

public interface UpdatePetUseCase {
    PetResult updatePet(String id, Map<String, Object> changes, String caregiverId);
}
