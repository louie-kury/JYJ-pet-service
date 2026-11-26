package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PetDeletePersistenceAdapterTest {
    @Test void storageFailureIsNotReportedAsNotFoundOrSuccess() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("pets")))
            .thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> new PetDeletePersistenceAdapter(mongo).delete("pet", "owner", Instant.now()))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PERSISTENCE_UNAVAILABLE));
    }
}
