package com.louiekury.pet.vaccination;

import com.louiekury.pet.global.security.AuthenticatedMember;
import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.adapter.in.web.PetRegistrationAuthInterceptor;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccination.adapter.in.web.VaccinationController;
import com.louiekury.pet.vaccination.application.CreateVaccinationService;
import com.louiekury.pet.vaccination.application.VaccinationPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VaccinationTest {
    private final VaccinationPort port = mock(VaccinationPort.class);
    private final CreateVaccinationService service = new CreateVaccinationService(port,
        Clock.fixed(Instant.parse("2026-11-26T00:00:00Z"), ZoneOffset.UTC));
    private final CreateVaccinationService.Request request = new CreateVaccinationService.Request(
        "vaccine-1", LocalDate.parse("2026-11-01"), null, null, null, null);

    @Test void savesOwnerRecordWithNameSnapshot() {
        when(port.isOwnedPet("pet-1", "member-1")).thenReturn(true);
        when(port.findVaccineName("vaccine-1")).thenReturn(Optional.of(new CreateVaccinationService.VaccineName("vaccine-1", "광견병")));
        when(port.saveIfOwned(any(), eq("member-1"))).thenReturn(true);
        var result = service.create("pet-1", "member-1", request);
        assertThat(result.vaccineName().name()).isEqualTo("광견병");
        assertThat(result.vaccinatedOn()).isEqualTo(LocalDate.parse("2026-11-01"));
        verify(port).saveIfOwned(result, "member-1");
    }
    @Test void responseTimestampMatchesMongoMillisecondPrecision() {
        var precise = new CreateVaccinationService(port, Clock.fixed(Instant.parse("2026-11-26T00:00:00.123456789Z"), ZoneOffset.UTC));
        when(port.isOwnedPet("pet-1", "member-1")).thenReturn(true);
        when(port.findVaccineName("vaccine-1")).thenReturn(Optional.of(new CreateVaccinationService.VaccineName("vaccine-1", "광견병")));
        when(port.saveIfOwned(any(), eq("member-1"))).thenReturn(true);
        assertThat(precise.create("pet-1", "member-1", request).createdAt()).isEqualTo(Instant.parse("2026-11-26T00:00:00.123Z"));
    }
    @Test void rejectsOtherOwnerBeforeLookingUpVaccine() {
        assertCode(() -> service.create("pet-1", "other", request), ErrorCode.NOT_FOUND);
        verify(port, never()).findVaccineName(any());
    }
    @Test void rejectsMissingVaccineWithoutSaving() {
        when(port.isOwnedPet("pet-1", "member-1")).thenReturn(true);
        assertCode(() -> service.create("pet-1", "member-1", request), ErrorCode.VACCINE_NAME_NOT_FOUND);
        verify(port, never()).saveIfOwned(any(), any());
    }
    @Test void rejectsPetDeletedBetweenCheckAndWrite() {
        when(port.isOwnedPet("pet-1", "member-1")).thenReturn(true);
        when(port.findVaccineName(any())).thenReturn(Optional.of(new CreateVaccinationService.VaccineName("vaccine-1", "광견병")));
        assertCode(() -> service.create("pet-1", "member-1", request), ErrorCode.NOT_FOUND);
    }
    @Test void rejectsMissingRequiredFields() {
        assertCode(() -> service.create("pet-1", "member-1", new CreateVaccinationService.Request(null, LocalDate.now(), null, null, null, null)), ErrorCode.VALIDATION_ERROR);
        assertCode(() -> service.create("pet-1", "member-1", new CreateVaccinationService.Request("v", null, null, null, null, null)), ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(port);
    }
    @Test void webChecksIdentityAndRejectsMalformedFields() throws Exception {
        var auth = mock(AuthenticatedMember.class);
        when(auth.require(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        when(auth.require("Bearer valid")).thenReturn("member-1");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new VaccinationController(service))
            .addInterceptors(new PetRegistrationAuthInterceptor(auth)).setControllerAdvice(new GlobalExceptionHandler()).build();
        String url = "/api/pets/pet-1/vaccinations";
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(url).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
            .content("{\"vaccineNameId\":\"vaccine-1\",\"vaccinatedOn\":\"bad\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(url).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
            .content("{\"vaccineNameId\":\"vaccine-1\",\"vaccinatedOn\":\"2026-11-01\",\"doseNumber\":1.5}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(port);
    }
    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
