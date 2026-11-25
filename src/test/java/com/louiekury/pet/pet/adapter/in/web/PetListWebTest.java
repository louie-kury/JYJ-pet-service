package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.pet.application.updatepet.UpdatePetUseCase;

import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.application.createpet.CreatePetUseCase;
import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.GetPetsService;
import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.pet.application.getpets.PetListPage;
import com.louiekury.pet.pet.application.port.out.GetPetsPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetListWebTest {

    private GetPetsPort pets;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        pets = mock(GetPetsPort.class);
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(), 0));
        GetPetsService service = new GetPetsService(pets,
            Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(
                new PetController(new PetWebAdapter(mock(CreatePetUseCase.class), service, mock(UpdatePetUseCase.class))))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("기본 요청은 0페이지 4개와 공통 페이지 응답을 반환한다")
    void usesDefaultPageSizeOfFour() throws Exception {
        mvc.perform(get("/api/pets"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"data\":{\"content\":[],\"page\":0,\"size\":4,\"totalElements\":0,\"totalPages\":0,\"hasNext\":false}}"));
        ArgumentCaptor<GetPetsQuery> query = ArgumentCaptor.forClass(GetPetsQuery.class);
        verify(pets).getPets(query.capture());
        assertThat(query.getValue().getPage()).isZero();
        assertThat(query.getValue().getSize()).isEqualTo(4);
    }

    @Test
    @DisplayName("API는 화면 기본값과 별개로 최대 100개 요청을 허용한다")
    void acceptsMaximumSize() throws Exception {
        mvc.perform(get("/api/pets").param("size", "100"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("임의 사용자 ID 쿼리·헤더가 있어도 임시 목록의 페이지 조건만 전달한다")
    void ignoresSpoofedOwnerIdentifiers() throws Exception {
        mvc.perform(get("/api/pets")
                .param("caregiverId", "victim").header("X-User-Id", "victim"))
            .andExpect(status().isOk());
        ArgumentCaptor<GetPetsQuery> query = ArgumentCaptor.forClass(GetPetsQuery.class);
        verify(pets).getPets(query.capture());
    }

    @Test
    @DisplayName("회원 연동 전 임시 목록은 인증 없이 정상 조회한다")
    void supportsTemporaryAnonymousListing() throws Exception {
        mvc.perform(get("/api/pets").header("X-User-Id", "owner-a")
                .header("Authorization", "Bearer unverified"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
        verify(pets).getPets(any(GetPetsQuery.class));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "page,abc", "page,2147483648", "size,0", "size,-1", "size,101", "size,abc"})
    @DisplayName("페이지 조건의 범위·타입 오류는 400이며 DB를 조회하지 않는다")
    void rejectsInvalidPagination(String field, String value) throws Exception {
        mvc.perform(get("/api/pets").param(field, value))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(pets);
    }

    @Test
    @DisplayName("선택 정보 null과 목록 요약 필드는 노출하고 내부 저장 필드는 노출하지 않는다")
    void returnsSummaryWithExplicitNulls() throws Exception {
        when(pets.getPets(any())).thenReturn(PetListPage.of(List.of(
            PetListData.of("pet-1", "루이", PetSpecies.DOG, null, null, null, null, "MALE")), 1));
        String body = mvc.perform(get("/api/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].petId").value("pet-1"))
            .andExpect(jsonPath("$.data.content[0].name").value("루이"))
            .andExpect(jsonPath("$.data.content[0].species.speciesId").value("dog"))
            .andExpect(jsonPath("$.data.content[0].species.name").value("강아지"))
            .andExpect(jsonPath("$.data.content[0].sex").value("MALE"))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"breed\":null", "\"profileMedia\":null", "\"age\":null")
            .doesNotContain("caregiverId", "registration", "birth", "_class", "createdAt");
    }

    @Test
    @DisplayName("조회 장애는 내부 원문 없이 503 공통 오류로 반환한다")
    void hidesPersistenceErrorDetails() throws Exception {
        when(pets.getPets(any())).thenThrow(new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE,
            new IllegalStateException("secret-uri-and-host")));
        String body = mvc.perform(get("/api/pets"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("PET_QUERY_UNAVAILABLE"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("secret-uri-and-host", "IllegalStateException");
    }
}
