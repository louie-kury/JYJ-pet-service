package com.louiekury.pet.vaccination.application;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateVaccinationService {
    private final VaccinationPort port;
    private final Clock clock;

    public record Request(String vaccineNameId, LocalDate vaccinatedOn, LocalDate nextVaccinationOn,
            Integer doseNumber, String hospitalName, String memo) { }
    public record VaccineName(String vaccineNameId, String name) { }
    public record Result(String vaccinationId, String petId, VaccineName vaccineName, LocalDate vaccinatedOn,
            LocalDate nextVaccinationOn, Integer doseNumber, String hospitalName, String memo,
            Instant createdAt, Instant updatedAt) { }

    public Result create(String petId, String caregiverId, Request request) {
        if (caregiverId == null || caregiverId.isBlank()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if (request == null || request.vaccineNameId() == null || request.vaccineNameId().isBlank()
                || request.vaccinatedOn() == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (!port.isOwnedPet(petId, caregiverId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        VaccineName name = port.findVaccineName(request.vaccineNameId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VACCINE_NAME_NOT_FOUND));
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        Result result = new Result(UUID.randomUUID().toString(), petId, name, request.vaccinatedOn(),
            request.nextVaccinationOn(), request.doseNumber(), request.hospitalName(), request.memo(), now, now);
        if (!port.saveIfOwned(result, caregiverId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        return result;
    }
}
