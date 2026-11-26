package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.security.AuthenticatedMember;
import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PetUpdateAvailabilityTest {
    @Test void authenticationPrecedesBodyParsing() throws Exception {
        var auth = mock(AuthenticatedMember.class);
        var adapter = mock(PetWebAdapter.class);
        when(auth.require(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        var mvc = MockMvcBuilders.standaloneSetup(new PetUpdateController(adapter))
            .addInterceptors(new PetRegistrationAuthInterceptor(auth))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(adapter);
    }
    @Test void verifiedIdentityIsPassedToUpdate() throws Exception {
        var auth = mock(AuthenticatedMember.class);
        var adapter = mock(PetWebAdapter.class);
        when(auth.require("Bearer valid")).thenReturn("owner-a");
        var mvc = MockMvcBuilders.standaloneSetup(new PetUpdateController(adapter))
            .addInterceptors(new PetRegistrationAuthInterceptor(auth))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(patch("/api/pets/pet-1").header("Authorization", "Bearer valid")
                .header("X-User-Id", "attacker").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"루이\"}"))
            .andExpect(status().isOk());
        verify(adapter).updatePet(eq("pet-1"), any(), eq("owner-a"));
    }
}
