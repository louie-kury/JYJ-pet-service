package com.louiekury.pet.registration.application.service;

import com.louiekury.pet.registration.application.model.PetResult;
import com.louiekury.pet.registration.application.model.RegisterPetCommand;
import com.louiekury.pet.registration.application.port.in.RegisterPetUseCase;
import com.louiekury.pet.registration.application.port.out.InsertPetPort;
import com.louiekury.pet.registration.application.port.out.LoadPetSpeciesPort;
import com.louiekury.pet.registration.domain.Pet;
import com.louiekury.pet.registration.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterPetService implements RegisterPetUseCase {

    private final LoadPetSpeciesPort speciesPort;
    private final InsertPetPort insertPort;
    private final Clock clock;

    @Override
    public PetResult register(RegisterPetCommand command) {
        String key = command.getIdempotencyKey();
        if (key != null && !key.matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        LocalDate today = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        Pet pet = Pet.create(command.getName(), command.getSpeciesId(), command.getBreedId(),
            command.getProfileMediaId(), command.getBirth(), command.getSex(),
            command.getIsNeutered(), command.getWeight(), command.getBasicVaccinationCompleted(), today);
        if (pet.getProfileMediaId() != null) {
            throw new BusinessException(ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
        }
        PetSpecies species = speciesPort.findById(pet.getSpeciesId())
            .filter(PetSpecies::isSupported)
            .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
        if (pet.getBreedId() != null && species.findBreedName(pet.getBreedId()).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        PetResult result = PetResult.from(pet, species, now, today);
        insertPort.insert(result, key);
        return result;
    }
}
