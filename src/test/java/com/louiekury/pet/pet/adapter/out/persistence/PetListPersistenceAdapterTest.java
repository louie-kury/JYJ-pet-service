package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetListPersistenceAdapterTest {

    private MongoTemplate template;
    private PetListPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        template = mock(MongoTemplate.class);
        adapter = new PetListPersistenceAdapter(template);
    }

    @Test
    @DisplayName("목록·전체 개수에 소유자 없음·삭제 없음 조건을 동일하게 적용하고 페이지 조건은 목록에만 적용한다")
    void constructsUnownedActiveFilterForBothQueries() {
        when(template.count(any(Query.class), eq("pets"))).thenReturn(0L);
        when(template.find(any(Query.class), eq(Document.class), eq("pets"))).thenReturn(List.of());

        adapter.getPets(GetPetsQuery.of(1, 4));

        ArgumentCaptor<Query> count = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> content = ArgumentCaptor.forClass(Query.class);
        verify(template).count(count.capture(), eq("pets"));
        verify(template).find(content.capture(), eq(Document.class), eq("pets"));
        Document expectedFilter = new Document("caregiverId", null).append("deletedAt", null);
        assertThat(count.getValue().getQueryObject()).isEqualTo(expectedFilter);
        assertThat(content.getValue().getQueryObject()).isEqualTo(expectedFilter);
        assertThat(count.getValue().getSkip()).isZero();
        assertThat(count.getValue().getLimit()).isZero();
        assertThat(content.getValue().getSkip()).isEqualTo(4);
        assertThat(content.getValue().getLimit()).isEqualTo(4);
        assertThat(content.getValue().getSortObject()).isEqualTo(new Document("createdAt", -1).append("_id", -1));
    }

    @Test
    @DisplayName("전체 개수 DB 장애는 정상 빈 목록으로 바꾸지 않는다")
    void convertsCountFailure() {
        when(template.count(any(Query.class), eq("pets")))
            .thenThrow(new DataAccessResourceFailureException("secret-db-host"));
        assertCode(() -> adapter.getPets(GetPetsQuery.of(0, 4)), ErrorCode.PET_QUERY_UNAVAILABLE);
    }

    @Test
    @DisplayName("목록 DB 장애는 전체 개수 조회 성공 여부와 무관하게 오류로 반환한다")
    void convertsContentFailure() {
        when(template.count(any(Query.class), eq("pets"))).thenReturn(1L);
        when(template.find(any(Query.class), eq(Document.class), eq("pets")))
            .thenThrow(new DataAccessResourceFailureException("secret-db-host"));
        assertCode(() -> adapter.getPets(GetPetsQuery.of(0, 4)), ErrorCode.PET_QUERY_UNAVAILABLE);
    }

    @Test
    @DisplayName("품종 조회 DB 장애를 snapshot 조회 부재와 혼동하지 않는다")
    void doesNotHideBreedDatabaseFailure() {
        Document pet = new Document("_id", "pet").append("name", "루이").append("speciesId", "dog")
            .append("sex", "MALE").append("breedId", "poodle")
            .append("registration", new Document("responseSnapshot", new Document("breed",
                new Document("breedId", "poodle").append("name", "푸들"))));
        when(template.count(any(Query.class), eq("pets"))).thenReturn(1L);
        when(template.find(any(Query.class), eq(Document.class), eq("pets"))).thenReturn(List.of(pet));
        when(template.find(any(Query.class), eq(Document.class), eq("pet_species")))
            .thenThrow(new DataAccessResourceFailureException("secret-db-host"));
        assertCode(() -> adapter.getPets(GetPetsQuery.of(0, 4)), ErrorCode.PET_QUERY_UNAVAILABLE);
    }

    private static void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
