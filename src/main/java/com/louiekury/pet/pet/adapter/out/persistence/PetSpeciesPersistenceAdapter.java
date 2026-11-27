package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.port.out.GetPetSpeciesPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetSpeciesPersistenceAdapter implements GetPetSpeciesPort {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<PetSpecies> getPetSpecies(String speciesId) {
        try {
            List<PetSpecies> matches = jdbc.query("SELECT uuid, name FROM species WHERE BINARY uuid = BINARY ?",
                (row, index) -> new PetSpecies(row.getString("uuid"), row.getString("name")), speciesId);
            if (matches.size() > 1 || matches.stream().anyMatch(item -> item.getSpeciesId() == null
                    || item.getSpeciesId().isBlank() || item.getName() == null || item.getName().isBlank())) {
                throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
            }
            return matches.stream().findFirst();
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE, exception);
        }
    }
}
