package com.louiekury.pet.pet.application.getpet;

import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.pet.application.port.in.GetPetUseCase;
import com.louiekury.pet.pet.application.port.out.GetPetPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPetService implements GetPetUseCase {
    private final GetPetPort port;
    private final Clock clock;

    public PetResult getPet(String id, String caregiverId) {
        if (caregiverId == null || caregiverId.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        GetPetPort.Data data = port.getPet(id, caregiverId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        PetListData pet = data.getSummary();
        if (pet.getProfileMediaId() != null) {
            throw new BusinessException(ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
        }
        LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        LocalDate birth = pet.getBirth();
        if (birth != null && (birth.getYear() < 1 || birth.getYear() > 9999 || birth.isAfter(today))) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE);
        }
        return PetResult.updated(pet.getPetId(), pet.getName(), pet.getSpecies(), pet.getBreedId(),
            pet.getBreedName(), birth, birth == null ? null : Period.between(birth, today), pet.getSex(),
            data.getNeutered(), data.getWeight(), data.getVaccination(), data.getCreatedAt(), data.getUpdatedAt());
    }
}
