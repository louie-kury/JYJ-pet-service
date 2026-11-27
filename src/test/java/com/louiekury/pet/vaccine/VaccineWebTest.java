package com.louiekury.pet.vaccine;

import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccine.adapter.in.web.VaccineController;
import com.louiekury.pet.vaccine.adapter.in.web.VaccineWebAdapter;
import com.louiekury.pet.vaccine.application.createvaccine.CreateVaccineService;
import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import com.louiekury.pet.vaccine.application.listvaccines.ListVaccinesService;
import com.louiekury.pet.vaccine.application.port.out.CreateVaccinePort;
import com.louiekury.pet.vaccine.application.port.out.ListVaccinesPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VaccineWebTest {
    private CreateVaccinePort port;
    private ListVaccinesPort listPort;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        port = mock(CreateVaccinePort.class);
        listPort = mock(ListVaccinesPort.class);
        mvc = MockMvcBuilders.standaloneSetup(new VaccineController(new VaccineWebAdapter(
            new CreateVaccineService(port), new ListVaccinesService(listPort))))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listsAllVaccinesWithoutAuthentication() throws Exception {
        when(listPort.listVaccines()).thenReturn(List.of(new VaccineResult("id-1", "광견병"),
            new VaccineResult("id-2", "종합백신")));
        mvc.perform(get("/api/vaccine-names"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].vaccineNameId").value("id-1"))
            .andExpect(jsonPath("$.data[0].name").value("광견병"))
            .andExpect(jsonPath("$.data[1].vaccineNameId").value("id-2"))
            .andExpect(jsonPath("$.data[1].name").value("종합백신"));
    }

    @Test
    void returnsEmptyListWhenNoVaccinesExist() throws Exception {
        when(listPort.listVaccines()).thenReturn(List.of());
        mvc.perform(get("/api/vaccine-names"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void listDatabaseFailureReturnsServiceUnavailable() throws Exception {
        when(listPort.listVaccines()).thenThrow(new BusinessException(ErrorCode.VACCINE_QUERY_UNAVAILABLE));
        mvc.perform(get("/api/vaccine-names"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("VACCINE_QUERY_UNAVAILABLE"));
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
