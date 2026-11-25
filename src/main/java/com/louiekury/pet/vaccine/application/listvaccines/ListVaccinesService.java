package com.louiekury.pet.vaccine.application.listvaccines;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListVaccinesService implements ListVaccinesUseCase {
    private final ListVaccinesPort port;

    @Override
    public List<VaccineResult> listVaccines() {
        return port.listVaccines();
    }
}
