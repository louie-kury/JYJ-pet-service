package com.louiekury.pet.vaccine.application.getvaccines;

import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import com.louiekury.pet.vaccine.application.port.in.GetVaccinesUseCase;
import com.louiekury.pet.vaccine.application.port.out.GetVaccinesPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetVaccinesService implements GetVaccinesUseCase {
    private final GetVaccinesPort port;

    @Override
    public List<VaccineResult> getVaccines() {
        return port.getVaccines();
    }
}
