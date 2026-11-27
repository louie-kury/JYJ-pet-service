package com.louiekury.pet.vaccine.application.port.in;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;

public interface CreateVaccineUseCase {
    VaccineResult createVaccine(String name);
}
