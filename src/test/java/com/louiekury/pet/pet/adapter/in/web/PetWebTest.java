package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.application.port.out.CreatePetPort;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.application.createpet.CreatePetService;
import com.louiekury.pet.pet.application.getpets.GetPetsUseCase;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PetWebTest {

    private CreatePetPort insert;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        insert = mock(CreatePetPort.class);
        GetPetBreedPort breeds = mock(GetPetBreedPort.class);
        CreatePetService service = new CreatePetService(breeds, insert,
            Clock.fixed(Instant.parse("2026-11-22T15:30:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new PetController(new PetWebAdapter(service, mock(GetPetsUseCase.class))))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("고양이 enum의 외부 ID와 표시 이름을 기존 응답 구조로 반환한다")
    void preservesCatResponseContract() throws Exception {
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"루이\",\"speciesId\":\"cat\",\"sex\":\"MALE\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.species.speciesId").value("cat"))
            .andExpect(jsonPath("$.data.species.name").value("고양이"))
            .andExpect(jsonPath("$.data.species.code").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOG", "CAT", "rabbit"})
    @DisplayName("API는 enum 이름이나 미지원 종을 외부 ID 별칭으로 허용하지 않는다")
    void rejectsSpeciesAliases(String speciesId) throws Exception {
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"루이\",\"speciesId\":\"" + speciesId + "\",\"sex\":\"MALE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("토큰과 사용자 없이 등록하면 201과 상세 및 한국시간 시각을 반환한다")
    void createsWithoutAuthentication() throws Exception {
        var response = mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "untrusted-user")
                .content("{\"name\":\"  루이  \",\"speciesId\":\"dog\",\"sex\":\"MALE\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("루이"))
            .andExpect(jsonPath("$.data.species.speciesId").value("dog"))
            .andExpect(jsonPath("$.data.species.name").value("강아지"))
            .andExpect(jsonPath("$.data.createdAt").value("2026-11-23T00:30:00+09:00"))
            .andExpect(jsonPath("$.data.updatedAt").value("2026-11-23T00:30:00+09:00"))
            .andExpect(jsonPath("$.data.caregiverId").doesNotExist())
            .andExpect(jsonPath("$.data.registration").doesNotExist())
            .andReturn().getResponse();
        assertThat(response.getHeader("Location")).startsWith("/api/pets/");
        assertThat(response.getContentAsString()).contains("\"birth\":null", "\"breed\":null",
            "\"age\":null", "\"profileMedia\":null", "\"isNeutered\":null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"speciesId\":\"dog\",\"sex\":\"MALE\"}",
        "{\"name\":\"루이\",\"sex\":\"MALE\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"caregiverId\":\"victim\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"createdAt\":\"2020-01-01\"}",
        "{\"name\":4,\"speciesId\":\"dog\",\"sex\":\"MALE\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"weight\":\"5.1\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"isNeutered\":\"false\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"basicVaccinationCompleted\":1}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"birth\":\"2023-02-29\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"birth\":\"2026-11-24\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"birth\":\"2023-5-1\"}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"weight\":0}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"weight\":200.00000000000000001}",
        "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"weight\":0.00100000000000000001}",
        "{broken"
    })
    @DisplayName("잘못된 필수값·주입 필드·타입·날짜는 400이며 저장하지 않는다")
    void rejectsMalformedOrInvalidInput(String json) throws Exception {
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(insert);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "has space", "한글", "!invalid"})
    @DisplayName("제공된 재시도 키가 형식에 맞지 않으면 저장하지 않는다")
    void rejectsInvalidKeys(String key) throws Exception {
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key).content(minimumJson()))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("미연동 프로필은 503을 반환하고 파일을 무시한 채 동물을 저장하지 않는다")
    void rejectsUnverifiedMedia() throws Exception {
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\",\"profileMediaId\":\"media\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("MEDIA_SERVICE_UNAVAILABLE"));
        verifyNoInteractions(insert);
    }

    @Test
    @DisplayName("익명 중복 키는 기존 상세를 노출하지 않고 409를 반환한다")
    void duplicateKeyDoesNotRevealExistingPet() throws Exception {
        doThrow(new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT)).when(insert).createPet(any(), eq("same-key"));
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "same-key").content(minimumJson()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(header().doesNotExist("Location"));
    }

    private static String minimumJson() {
        return "{\"name\":\"루이\",\"speciesId\":\"dog\",\"sex\":\"MALE\"}";
    }
}
