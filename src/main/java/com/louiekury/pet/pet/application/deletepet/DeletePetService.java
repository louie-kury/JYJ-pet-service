package com.louiekury.pet.pet.application.deletepet;

import com.louiekury.pet.pet.application.port.in.DeletePetUseCase;
import com.louiekury.pet.pet.application.port.out.DeletePetPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeletePetService implements DeletePetUseCase {
    private final DeletePetPort port;
    private final Clock clock;

    public void delete(String id, String caregiverId) {
        if (caregiverId == null || caregiverId.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!port.delete(id, caregiverId, clock.instant())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }
}
