package com.louiekury.pet.vaccine.application.port.out;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;

public interface CreateVaccinePort {
    void createVaccine(VaccineResult vaccine);
}
