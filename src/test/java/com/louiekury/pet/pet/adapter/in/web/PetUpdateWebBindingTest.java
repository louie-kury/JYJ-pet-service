package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.application.port.in.CreatePetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetsUseCase;
import com.louiekury.pet.pet.application.port.in.UpdatePetUseCase;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetUpdateWebBindingTest {
    @Test
    void passesNullAndOmittedFieldsToUseCase() throws Exception {
        UpdatePetUseCase update = mock(UpdatePetUseCase.class);
        doThrow(new BusinessException(ErrorCode.NOT_FOUND)).when(update).updatePet(eq("pet"), any(), eq("owner"));
        var adapter = new PetWebAdapter(mock(CreatePetUseCase.class), mock(GetPetsUseCase.class),
            update, mock(GetPetUseCase.class));
        var mvc = MockMvcBuilders.standaloneSetup(new PetUpdateController(adapter))
            .setMessageConverters(RequestTestConverter.create())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mvc.perform(patch("/api/pets/pet")
                .requestAttr(PetRegistrationAuthInterceptor.CAREGIVER_ID, "owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"breedId\":null,\"name\":\"루이\"}"))
            .andExpect(status().isNotFound());

        var changes = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(update).updatePet(eq("pet"), changes.capture(), eq("owner"));
        assertThat(changes.getValue()).containsEntry("breedId", null).containsEntry("name", "루이")
            .doesNotContainKey("birth");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"name\":null}", "{\"name\":\"   \"}",
        "{\"speciesId\":null}", "{\"speciesId\":\"\"}",
        "{\"sex\":null}", "{\"sex\":\"   \"}"
    })
    void rejectsPresentNullOrBlankRequiredFields(String body) throws Exception {
        UpdatePetUseCase update = mock(UpdatePetUseCase.class);
        var adapter = new PetWebAdapter(mock(CreatePetUseCase.class), mock(GetPetsUseCase.class),
            update, mock(GetPetUseCase.class));
        var mvc = MockMvcBuilders.standaloneSetup(new PetUpdateController(adapter))
            .setMessageConverters(RequestTestConverter.create())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mvc.perform(patch("/api/pets/pet")
                .requestAttr(PetRegistrationAuthInterceptor.CAREGIVER_ID, "owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(update);
    }
}
