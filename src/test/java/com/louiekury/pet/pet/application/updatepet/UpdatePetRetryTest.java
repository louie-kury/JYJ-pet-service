package com.louiekury.pet.pet.application.updatepet;

import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.application.port.out.UpdatePetPort;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdatePetRetryTest {
    private final UpdatePetPort port = mock(UpdatePetPort.class);
    private final Instant now = Instant.parse("2026-11-26T00:00:00Z");
    private final UpdatePetService service = new UpdatePetService(port, mock(GetPetBreedPort.class),
        Clock.fixed(now, ZoneOffset.UTC));
    private final UpdatePetPort.Data pet = new UpdatePetPort.Data("pet", "루이", "dog", null, null,
        null, null, "MALE", null, null, null, now, now);

    @Test void deletionBetweenFinalReadAndWriteReturnsNotFound() {
        when(port.find(eq("pet"), anyMap(), eq("owner")))
            .thenReturn(Optional.of(pet), Optional.of(pet), Optional.of(pet), Optional.empty());
        when(port.update(any(), anyMap(), any(), eq("owner"))).thenReturn(false);

        assertError(ErrorCode.NOT_FOUND);
        verify(port, times(3)).update(any(), anyMap(), any(), eq("owner"));
    }

    @Test void repeatedVersionConflictStillReturnsPersistenceUnavailable() {
        when(port.find(eq("pet"), anyMap(), eq("owner"))).thenReturn(Optional.of(pet));
        when(port.update(any(), anyMap(), any(), eq("owner"))).thenReturn(false);

        assertError(ErrorCode.PERSISTENCE_UNAVAILABLE);
    }

    private void assertError(ErrorCode expected) {
        assertThatThrownBy(() -> service.updatePet("pet", new HashMap<>(Map.of("name", "새 이름")), "owner"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
