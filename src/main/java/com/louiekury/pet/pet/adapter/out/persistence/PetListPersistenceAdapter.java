package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.pet.application.getpets.PetListPage;
import com.louiekury.pet.pet.application.port.out.GetPetsPort;
import com.louiekury.pet.pet.application.port.out.GetPetSpeciesPort;
import com.louiekury.pet.pet.application.port.out.GetPetBreedPort;
import com.louiekury.pet.pet.domain.PetBreed;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetListPersistenceAdapter implements GetPetsPort {

    private final MongoTemplate mongoTemplate;
    private final GetPetSpeciesPort speciesPort;
    private final GetPetBreedPort breedPort;

    @Override
    public PetListPage getPets(GetPetsQuery query) {
        try {
            Criteria owner = Criteria.where("caregiverId").is(query.getCaregiverId()).and("deletedAt").is(null);
            long total = mongoTemplate.count(Query.query(owner), "pets");
            Query page = Query.query(owner).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"))
                .skip(query.getOffset()).limit(query.getSize());
            page.fields().include("_id", "name", "speciesId", "breedId", "profileMediaId", "birth", "sex",
                "registration.responseSnapshot.breed", "registration.responseSnapshot.species.speciesId");
            List<Document> pets = mongoTemplate.find(page, Document.class, "pets");
            CatalogCache catalogs = getCatalogs(pets);
            return PetListPage.of(pets.stream().map(pet -> getPetData(pet, catalogs)).toList(), total);
        } catch (DataAccessException | MongoException exception) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE, exception);
        }
    }

    static class CatalogCache {
        final Map<String, PetSpecies> species;
        final Map<String, PetBreed> breeds;

        CatalogCache(Map<String, PetSpecies> species, Map<String, PetBreed> breeds) {
            this.species = species;
            this.breeds = breeds;
        }
    }

    CatalogCache getCatalogs(List<Document> pets) {
        Set<String> speciesIds = pets.stream().map(pet -> getString(pet, "speciesId", false))
            .collect(Collectors.toSet());
        Set<String> breedIds = pets.stream().map(pet -> getString(pet, "breedId", true))
            .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<String, PetSpecies> species = speciesPort.getPetSpeciesByIds(speciesIds);
        return new CatalogCache(species, breedPort.getPetBreedsByIds(species, breedIds));
    }

    PetListData getPetData(Document pet, CatalogCache catalogs) {
        String speciesId = getString(pet, "speciesId", false);
        PetSpecies species = catalogs.species.get(speciesId);
        if (species == null) {
            throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
        }
        String breedId = getString(pet, "breedId", true);
        String breedName = breedId == null ? null : getBreedName(pet, species, breedId, catalogs);
        String birth = getString(pet, "birth", true);
        LocalDate birthday;
        try {
            birthday = birth == null ? null : LocalDate.parse(birth);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE, exception);
        }
        String sex = getString(pet, "sex", false);
        if (!"MALE".equals(sex) && !"FEMALE".equals(sex)) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE);
        }
        return PetListData.of(getString(pet, "_id", false), getString(pet, "name", false), species,
            breedId, breedName, getString(pet, "profileMediaId", true), birthday, sex);
    }

    private String getBreedName(Document pet, PetSpecies species, String breedId, CatalogCache catalogs) {
        PetBreed found = catalogs.breeds.get(breedId);
        if (found != null && species.getSpeciesId().equals(found.getSpecies().getSpeciesId())) {
            return found.getName();
        }
        if (pet.get("registration") instanceof Map<?, ?> registration
                && registration.get("responseSnapshot") instanceof Map<?, ?> snapshot
                && snapshot.get("species") instanceof Map<?, ?> savedSpecies
                && species.getSpeciesId().equals(savedSpecies.get("speciesId"))
                && snapshot.get("breed") instanceof Map<?, ?> breed
                && breedId.equals(breed.get("breedId"))
                && breed.get("name") instanceof String name && !name.isBlank()) {
            return name;
        }
        throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
    }

    private String getString(Document document, String field, boolean nullable) {
        Object value = document.get(field);
        if (nullable && value == null) {
            return null;
        }
        if (!(value instanceof String string) || string.isBlank()) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE);
        }
        return string;
    }
}
