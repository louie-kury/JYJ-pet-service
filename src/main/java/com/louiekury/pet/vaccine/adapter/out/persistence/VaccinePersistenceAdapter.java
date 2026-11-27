package com.louiekury.pet.vaccine.adapter.out.persistence;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import com.louiekury.pet.vaccine.application.port.out.CreateVaccinePort;
import com.louiekury.pet.vaccine.application.port.out.GetVaccinesPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VaccinePersistenceAdapter implements CreateVaccinePort, GetVaccinesPort {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Override
    public List<VaccineResult> getVaccines() {
        try {
            return jdbc.query("SELECT uuid, name FROM vaccine ORDER BY id",
                (row, index) -> new VaccineResult(row.getString("uuid"), row.getString("name")));
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.VACCINE_QUERY_UNAVAILABLE, exception);
        }
    }

    @Override
    public void createVaccine(VaccineResult vaccine) {
        try {
            jdbc.update("INSERT INTO vaccine (uuid, name, created_at) VALUES (?, ?, ?)",
                vaccine.getVaccineNameId(), vaccine.getName(),
                LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul")));
        } catch (DuplicateKeyException exception) {
            try {
                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM vaccine WHERE name = ?",
                    Integer.class, vaccine.getName());
                if (count != null && count > 0) {
                    throw new BusinessException(ErrorCode.VACCINE_NAME_DUPLICATE);
                }
            } catch (DataAccessException queryFailure) {
                throw new BusinessException(ErrorCode.VACCINE_PERSISTENCE_UNAVAILABLE, queryFailure);
            }
            throw new BusinessException(ErrorCode.VACCINE_PERSISTENCE_UNAVAILABLE, exception);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.VACCINE_PERSISTENCE_UNAVAILABLE, exception);
        }
    }
}
