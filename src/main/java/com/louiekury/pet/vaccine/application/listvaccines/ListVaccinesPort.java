package com.louiekury.pet.vaccine.application.listvaccines;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import java.util.List;

public interface ListVaccinesPort {
    List<VaccineResult> listVaccines();
}
