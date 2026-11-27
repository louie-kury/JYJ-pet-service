package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.domain.PetBreed;
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
public class PetBreedPersistenceAdapter implements GetPetBreedPort {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<PetBreed> getPetBreedBySpeciesAndId(PetSpecies species, String breedId) {
        try {
            List<PetBreed> matches = jdbc.query(
                "SELECT b.uuid, b.name FROM breed b JOIN species s ON s.id = b.species_id "
                    + "WHERE BINARY s.uuid = BINARY ? AND BINARY b.uuid = BINARY ?",
                (row, index) -> new PetBreed(species, row.getString("uuid"), row.getString("name")),
                species.getSpeciesId(), breedId);
            if (matches.isEmpty()) {
                Integer orphanCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM breed b LEFT JOIN species s ON s.id = b.species_id "
                        + "WHERE BINARY b.uuid = BINARY ? AND s.id IS NULL", Integer.class, breedId);
                if (orphanCount != null && orphanCount > 0) {
                    throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
                }
            }
            if (matches.size() > 1 || matches.stream().anyMatch(item -> item.getBreedId() == null
                    || item.getBreedId().isBlank() || item.getName() == null || item.getName().isBlank())) {
                throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
            }
            return matches.stream().findFirst();
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE, exception);
        }
    }
}
