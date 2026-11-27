package com.louiekury.pet.pet.application.updatepet;

import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.port.in.UpdatePetUseCase;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.application.port.out.UpdatePetPort;
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
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdatePetService implements UpdatePetUseCase {
    private final UpdatePetPort port;
    private final GetPetBreedPort breedPort;
    private final Clock clock;

    @Override
    public PetResult updatePet(String id, Map<String, Object> changes, String caregiverId) {
        if (caregiverId == null || caregiverId.isBlank()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if (changes.isEmpty()) throw new BusinessException(ErrorCode.EMPTY_UPDATE);
        for (int attempt = 0; attempt < 3; attempt++) {
            UpdatePetPort.Data old = port.find(id, changes, caregiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
            if (!now.isAfter(old.updatedAt())) now = old.updatedAt().plusMillis(1);
            LocalDate today = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
            String name = (String) changes.getOrDefault("name", old.name());
            name = name == null ? null : name.strip();
            require(name != null && !name.isBlank() && name.codePointCount(0, name.length()) <= 50);
            String speciesId = (String) changes.getOrDefault("speciesId", old.speciesId());
            PetSpecies species = PetSpecies.fromId(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
            String breedId = (String) changes.getOrDefault("breedId", old.breedId());
            require(breedId == null || !breedId.isBlank());
            String media = (String) changes.getOrDefault("profileMediaId", old.profileMediaId());
            require(media == null || !media.isBlank());
            if (media != null) throw new BusinessException(ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
            String sex = (String) changes.getOrDefault("sex", old.sex());
            require("MALE".equals(sex) || "FEMALE".equals(sex));
            LocalDate birth = (LocalDate) changes.getOrDefault("birth", old.birth());
            require(birth == null || (birth.getYear() >= 1 && birth.getYear() <= 9999 && !birth.isAfter(today)));
            BigDecimal weight = (BigDecimal) changes.getOrDefault("weight", old.weight());
            require(weight == null || (weight.signum() > 0 && weight.compareTo(new BigDecimal("200")) <= 0
                && weight.stripTrailingZeros().scale() <= 3));
            String breedName = old.breedName();
            if (breedId == null) {
                breedName = null;
            } else if (!Objects.equals(speciesId, old.speciesId()) || !Objects.equals(breedId, old.breedId())) {
                PetBreed breed = breedPort.getPetBreedBySpeciesAndId(species, breedId)
                    .filter(found -> found.getSpecies() == species && found.getBreedId().equals(breedId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
                breedName = breed.getName();
            } else if (breedName == null) {
                throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
            }
            if (changes.containsKey("name")) changes.put("name", name);
            if (port.update(old, changes, now, caregiverId)) {
                Boolean neutered = (Boolean) changes.getOrDefault("isNeutered", old.neutered());
                Boolean vaccination = (Boolean) changes.getOrDefault("basicVaccinationCompleted", old.vaccination());
                return PetResult.updated(id, name, species, breedId, breedName, birth,
                    birth == null ? null : Period.between(birth, today), sex, neutered, weight,
                    vaccination, old.createdAt(), now);
            }
        }
        if (port.find(id, changes, caregiverId).isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND);
        throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE);
    }
    private static void require(boolean condition) {
        if (!condition) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
