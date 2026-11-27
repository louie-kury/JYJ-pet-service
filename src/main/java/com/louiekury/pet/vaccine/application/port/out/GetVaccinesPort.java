package com.louiekury.pet.vaccine.application.port.out;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import java.util.List;

public interface GetVaccinesPort {
    List<VaccineResult> getVaccines();
}
