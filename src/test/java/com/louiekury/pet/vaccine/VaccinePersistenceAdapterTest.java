package com.louiekury.pet.vaccine;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccine.adapter.out.persistence.VaccinePersistenceAdapter;
import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VaccinePersistenceAdapterTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final VaccinePersistenceAdapter adapter = new VaccinePersistenceAdapter(jdbc, Clock.systemUTC());
    private final VaccineResult vaccine = new VaccineResult("8477f925-a550-495f-b48b-3ec138098a35", "광견병");

    @Test
    void duplicateNameIsConflict() {
        when(jdbc.update(anyString(), any(), any(), any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("광견병"))).thenReturn(1);
        assertThatThrownBy(() -> adapter.createVaccine(vaccine))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VACCINE_NAME_DUPLICATE);
    }

    @Test
    void databaseFailureIsUnavailable() {
        when(jdbc.update(anyString(), any(), any(), any())).thenThrow(new DataAccessResourceFailureException("down"));
        assertThatThrownBy(() -> adapter.createVaccine(vaccine))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VACCINE_PERSISTENCE_UNAVAILABLE);
    }

    @Test
    void duplicateUuidIsNotReportedAsDuplicateName() {
        when(jdbc.update(anyString(), any(), any(), any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("광견병"))).thenReturn(0);
        assertThatThrownBy(() -> adapter.createVaccine(vaccine))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VACCINE_PERSISTENCE_UNAVAILABLE);
    }
}
