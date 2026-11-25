package com.louiekury.pet.vaccine.application.createvaccine;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateVaccineService implements CreateVaccineUseCase {
    private final CreateVaccinePort port;

    @Override
    public VaccineResult createVaccine(String name) {
        String normalized = name == null ? null : name.strip();
        if (normalized == null || normalized.isBlank()
                || normalized.codePointCount(0, normalized.length()) > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        VaccineResult result = new VaccineResult(UUID.randomUUID().toString(), normalized);
        port.createVaccine(result);
        return result;
    }
}
