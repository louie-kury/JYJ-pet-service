package com.louiekury.pet.pet.application.createpet;

import com.louiekury.pet.pet.application.port.out.CreatePetPort;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.domain.Pet;
import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreatePetService implements CreatePetUseCase {

    private final GetPetBreedPort breedPort;
    private final CreatePetPort insertPort;
    private final Clock clock;

    @Override
    public PetResult createPet(CreatePetCommand command) {
        String key = command.getIdempotencyKey();
        if (key != null && !key.matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        LocalDate today = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        String name = command.getName() == null ? null : command.getName().strip();
        require(name != null && !name.isBlank() && name.codePointCount(0, name.length()) <= 50);
        PetSpecies species = PetSpecies.fromId(command.getSpeciesId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
        require(command.getBreedId() == null || !command.getBreedId().isBlank());
        require(command.getProfileMediaId() == null || !command.getProfileMediaId().isBlank());
        require("MALE".equals(command.getSex()) || "FEMALE".equals(command.getSex()));
        LocalDate birth = command.getBirth();
        require(birth == null || (birth.getYear() >= 1 && birth.getYear() <= 9999 && !birth.isAfter(today)));
        BigDecimal weight = command.getWeight();
        require(weight == null || (weight.signum() > 0 && weight.compareTo(new BigDecimal("200")) <= 0
            && weight.stripTrailingZeros().scale() <= 3));
        Pet pet = Pet.create(name, species, command.getBreedId(), command.getProfileMediaId(),
            birth, command.getSex(), command.getIsNeutered(), weight, command.getBasicVaccinationCompleted());
        if (pet.getProfileMediaId() != null) {
            throw new BusinessException(ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
        }
        PetBreed breed = null;
        if (pet.getBreedId() != null) {
            breed = breedPort.getPetBreedBySpeciesAndId(pet.getSpecies(), pet.getBreedId())
                .filter(found -> found.getSpecies() == pet.getSpecies()
                    && found.getBreedId().equals(pet.getBreedId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
        }
        Period age = birth == null ? null : Period.between(birth, today);
        PetResult result = PetResult.of(pet, breed, now, age);
        insertPort.createPet(result, key);
        return result;
    }
    private static void require(boolean condition) {
        if (!condition) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
