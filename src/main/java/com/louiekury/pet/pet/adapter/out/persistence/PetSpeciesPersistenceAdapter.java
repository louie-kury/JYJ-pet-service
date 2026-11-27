package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.port.out.GetPetSpeciesPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

    @Override
    public Map<String, PetSpecies> getPetSpeciesByIds(Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            List<PetSpecies> matches = new NamedParameterJdbcTemplate(jdbc).query(
                "SELECT uuid, name FROM species WHERE BINARY uuid IN (:ids)", Map.of("ids", ids),
                (row, index) -> new PetSpecies(row.getString("uuid"), row.getString("name")));
            Map<String, PetSpecies> result = new HashMap<>();
            for (PetSpecies item : matches) {
                if (item.getSpeciesId() == null || item.getSpeciesId().isBlank() || item.getName() == null
                        || item.getName().isBlank() || result.putIfAbsent(item.getSpeciesId(), item) != null) {
                    throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
                }
            }
            return result;
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE, exception);
        }
    }
}
