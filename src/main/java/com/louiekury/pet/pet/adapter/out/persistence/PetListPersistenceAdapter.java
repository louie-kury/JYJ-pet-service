package com.louiekury.pet.pet.adapter.out.persistence;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.PetListData;
import com.louiekury.pet.pet.application.getpets.PetListPage;
import com.louiekury.pet.pet.application.port.out.GetPetsPort;
import com.louiekury.pet.pet.domain.PetSpecies;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.mongodb.MongoException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
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

    @Override
    public PetListPage getPets(GetPetsQuery query) {
        try {
            // 회원 연결 전 임시 목록은 소유자 null 또는 필드가 없는 동물만 공개한다.
            Criteria owner = Criteria.where("caregiverId").is(null).and("deletedAt").is(null);
            long total = mongoTemplate.count(Query.query(owner), "pets");
            Query page = Query.query(owner).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"))
                .skip(query.getOffset()).limit(query.getSize());
            page.fields().include("_id", "name", "speciesId", "breedId", "profileMediaId", "birth", "sex",
                "registration.responseSnapshot.breed", "registration.responseSnapshot.species.speciesId");
            List<Document> pets = mongoTemplate.find(page, Document.class, "pets");
            Map<String, Document> catalogs = getBreedCatalogs(pets);
            return PetListPage.of(pets.stream().map(pet -> getPetData(pet, catalogs)).toList(), total);
        } catch (DataAccessException | MongoException exception) {
            throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE, exception);
        }
    }

    Map<String, Document> getBreedCatalogs(List<Document> pets) {
        Set<String> speciesIds = pets.stream().filter(pet -> pet.get("breedId") != null)
            .map(pet -> getString(pet, "speciesId", false)).collect(Collectors.toSet());
        Map<String, Document> catalogs = new HashMap<>();
        if (!speciesIds.isEmpty()) {
            Query query = Query.query(Criteria.where("_id").in(speciesIds));
            query.fields().include("_id", "breeds");
            for (Document catalog : mongoTemplate.find(query, Document.class, "pet_species")) {
                catalogs.put(getString(catalog, "_id", false), catalog);
            }
        }
        return catalogs;
    }

    PetListData getPetData(Document pet, Map<String, Document> catalogs) {
        String speciesId = getString(pet, "speciesId", false);
        PetSpecies species = PetSpecies.fromId(speciesId)
            .orElseThrow(() -> new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE));
        String breedId = getString(pet, "breedId", true);
        String breedName = breedId == null ? null : getBreedName(pet, breedId, catalogs.get(speciesId));
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

    private String getBreedName(Document pet, String breedId, Document catalog) {
        String found = null;
        if (catalog != null && catalog.get("breeds") instanceof List<?> breeds) {
            for (Object value : breeds) {
                if (value instanceof Map<?, ?> breed && breedId.equals(breed.get("breedId"))) {
                    if (found != null || !(breed.get("name") instanceof String name) || name.isBlank()) {
                        throw new BusinessException(ErrorCode.REFERENCE_DATA_UNAVAILABLE);
                    }
                    // 비활성 품종도 이미 등록된 동물의 표시에는 사용할 수 있다.
                    found = name;
                }
            }
        }
        if (found != null) {
            return found;
        }
        if (pet.get("registration") instanceof Map<?, ?> registration
                && registration.get("responseSnapshot") instanceof Map<?, ?> snapshot
                && snapshot.get("species") instanceof Map<?, ?> savedSpecies
                && getString(pet, "speciesId", false).equals(savedSpecies.get("speciesId"))
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
