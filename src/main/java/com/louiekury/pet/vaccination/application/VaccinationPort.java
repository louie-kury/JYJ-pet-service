package com.louiekury.pet.vaccination.application;

import java.util.Optional;

public interface VaccinationPort {
    boolean isOwnedPet(String petId, String caregiverId);
    Optional<CreateVaccinationService.VaccineName> findVaccineName(String id);
    boolean saveIfOwned(CreateVaccinationService.Result vaccination, String caregiverId);
}
