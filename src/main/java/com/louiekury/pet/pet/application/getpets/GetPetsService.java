package com.louiekury.pet.pet.application.getpets;

import com.louiekury.pet.pet.application.port.out.GetPetsPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPetsService implements GetPetsUseCase {

    private final GetPetsPort getPetsPort;
    private final Clock clock;

    @Override
    public GetPetsResult getPets(GetPetsQuery query) {
        if (query.getCaregiverId() == null || query.getCaregiverId().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (query.getPage() < 0 || query.getSize() < 1 || query.getSize() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        PetListPage pets = getPetsPort.getPets(query);
        List<PetListItemResult> content = pets.getContent().stream()
            .map(pet -> getPetResult(pet, today)).toList();
        return GetPetsResult.of(content, query, pets.getTotalElements());
    }

    private PetListItemResult getPetResult(PetListData pet, LocalDate today) {
        if (pet.getProfileMediaId() != null) {
            throw new BusinessException(ErrorCode.MEDIA_SERVICE_UNAVAILABLE);
        }
        if (pet.getBirth() != null && (pet.getBirth().getYear() < 1
                || pet.getBirth().getYear() > 9999 || pet.getBirth().isAfter(today))) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE);
        }
        Period age = pet.getBirth() == null ? null : Period.between(pet.getBirth(), today);
        return PetListItemResult.of(pet, age);
    }
}
