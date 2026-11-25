package com.louiekury.pet.vaccine;

import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccine.adapter.in.web.VaccineController;
import com.louiekury.pet.vaccine.adapter.in.web.VaccineWebAdapter;
import com.louiekury.pet.vaccine.application.createvaccine.CreateVaccinePort;
import com.louiekury.pet.vaccine.application.createvaccine.CreateVaccineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VaccineWebTest {
    private CreateVaccinePort port;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        port = mock(CreateVaccinePort.class);
        mvc = MockMvcBuilders.standaloneSetup(new VaccineController(new VaccineWebAdapter(
            new CreateVaccineService(port)))).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsVaccineWithoutAuthentication() throws Exception {
        mvc.perform(post("/api/vaccine-names").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"  광견병  \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.name").value("광견병"))
            .andExpect(jsonPath("$.data.vaccineNameId").isNotEmpty());
        verify(port).createVaccine(any());
    }

    @Test
    void rejectsEmptyAndInvalidInput() throws Exception {
        for (String body : new String[]{"{}", "{\"name\":null}", "{\"name\":12}", "{\"name\":\"  \"}",
                "{\"name\":\"" + "가".repeat(101) + "\"}"}) {
            mvc.perform(post("/api/vaccine-names").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(port);
    }

    @Test
    void acceptsHundredCharacters() throws Exception {
        mvc.perform(post("/api/vaccine-names").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + "가".repeat(100) + "\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void duplicateIsConflict() throws Exception {
        doThrow(new BusinessException(ErrorCode.VACCINE_NAME_DUPLICATE)).when(port).createVaccine(any());
        mvc.perform(post("/api/vaccine-names").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"광견병\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VACCINE_NAME_DUPLICATE"));
    }
}
