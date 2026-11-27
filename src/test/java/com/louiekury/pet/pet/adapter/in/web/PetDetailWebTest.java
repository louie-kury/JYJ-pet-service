package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.security.AuthenticatedMember;
import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.application.port.in.CreatePetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetsUseCase;
import com.louiekury.pet.pet.application.port.in.UpdatePetUseCase;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PetDetailWebTest {
    @Test void missingTokenIsRejectedBeforeQuery() throws Exception {
        var auth = mock(AuthenticatedMember.class);
        var detail = mock(GetPetUseCase.class);
        when(auth.require(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PetController(new PetWebAdapter(
            mock(CreatePetUseCase.class), mock(GetPetsUseCase.class), mock(UpdatePetUseCase.class), detail)))
            .addInterceptors(new PetRegistrationAuthInterceptor(auth))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/pets/pet-1").header("X-User-Id", "forged"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(detail);
    }
    @Test void authenticatedOwnerCanReadDetail() throws Exception {
        var auth = mock(AuthenticatedMember.class);
        var detail = mock(GetPetUseCase.class);
        when(auth.require("Bearer valid")).thenReturn("member-1");
        var now = java.time.Instant.parse("2026-11-26T00:00:00Z");
        when(detail.getPet("pet-1", "member-1")).thenReturn(com.louiekury.pet.pet.application.createpet.PetResult.updated(
            "pet-1", "루이", new com.louiekury.pet.pet.domain.PetSpecies("dog", "강아지"), null, null, null, null,
            "MALE", true, new java.math.BigDecimal("5.2"), false, now, now));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PetController(new PetWebAdapter(
            mock(CreatePetUseCase.class), mock(GetPetsUseCase.class), mock(UpdatePetUseCase.class), detail)))
            .addInterceptors(new PetRegistrationAuthInterceptor(auth))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/pets/pet-1").header("Authorization", "Bearer valid").header("X-User-Id", "attacker"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("루이"))
            .andExpect(jsonPath("$.data.weight").value(5.2)).andExpect(jsonPath("$.data.isNeutered").value(true));
        verify(detail).getPet("pet-1", "member-1");
    }
    @Test void registrationUsesVerifiedMemberInsteadOfHeader() throws Exception {
        var auth = mock(AuthenticatedMember.class);
        var insert = mock(com.louiekury.pet.pet.application.port.out.CreatePetPort.class);
        var service = new com.louiekury.pet.pet.application.createpet.CreatePetService(
            mock(com.louiekury.pet.pet.application.port.out.GetPetBreedPort.class), com.louiekury.pet.pet.support.CatalogFixtures.species(), insert,
            java.time.Clock.fixed(java.time.Instant.parse("2026-11-26T00:00:00Z"), java.time.ZoneOffset.UTC));
        when(auth.require("Bearer valid")).thenReturn("member-1");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PetController(new PetWebAdapter(
            service, mock(GetPetsUseCase.class), mock(UpdatePetUseCase.class), mock(GetPetUseCase.class))))
            .addInterceptors(new PetRegistrationAuthInterceptor(auth))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/pets")
                .header("Authorization", "Bearer valid").header("X-User-Id", "attacker")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\"}"))
            .andExpect(status().isCreated());
        verify(insert).createPet(any(), isNull(), eq("member-1"));
    }
}
