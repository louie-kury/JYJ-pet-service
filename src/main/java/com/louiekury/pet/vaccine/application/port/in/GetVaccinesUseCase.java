package com.louiekury.pet.vaccine.application.port.in;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import java.util.List;

public interface GetVaccinesUseCase {
    List<VaccineResult> getVaccines();
}
