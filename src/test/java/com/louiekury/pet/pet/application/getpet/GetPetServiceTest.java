package com.louiekury.pet.pet.application.getpet;

import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.pet.application.port.out.GetPetPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPetServiceTest {
    private final GetPetPort port = mock(GetPetPort.class);
    private final GetPetService service = new GetPetService(port, Clock.fixed(Instant.parse("2026-11-25T15:30:00Z"), ZoneOffset.UTC));
    private static final Instant CREATED = Instant.parse("2025-01-01T00:00:00Z");

    @Test void returnsStoredDetailAndAgeAtKoreanDate() {
        PetListData pet = PetListData.of("pet-1", "루이", new PetSpecies("dog", "강아지"), null, null, null, LocalDate.of(2020, 11, 26), "MALE");
        when(port.getPet("pet-1", "member-1")).thenReturn(Optional.of(new GetPetPort.Data(pet, true, null, false, CREATED, CREATED)));
        var result = service.getPet("pet-1", "member-1");
        assertThat(result.getName()).isEqualTo("루이");
        assertThat(result.getAge().getYears()).isEqualTo(6);
        assertThat(result.getAge().getMonths()).isZero();
        assertThat(result.getIsNeutered()).isTrue();
        assertThat(result.getBasicVaccinationCompleted()).isFalse();
    }
    @Test void missingOrOtherOwnersPetIsNotFound() {
        when(port.getPet("pet-1", "member-2")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPet("pet-1", "member-2")).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
    @Test void missingIdentityNeverQueriesDatabase() {
        assertThatThrownBy(() -> service.getPet("pet-1", null)).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verifyNoInteractions(port);
    }
    @Test void rejectsUnresolvedMedia() {
        PetListData pet = PetListData.of("pet-1", "루이", new PetSpecies("dog", "강아지"), null, null, "media-1", null, "MALE");
        when(port.getPet("pet-1", "member-1")).thenReturn(Optional.of(new GetPetPort.Data(pet, null, null, null, CREATED, CREATED)));
        assertThatThrownBy(() -> service.getPet("pet-1", "member-1")).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDIA_SERVICE_UNAVAILABLE));
    }
}
