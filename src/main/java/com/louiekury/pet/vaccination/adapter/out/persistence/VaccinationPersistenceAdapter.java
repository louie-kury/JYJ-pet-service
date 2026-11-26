package com.louiekury.pet.vaccination.adapter.out.persistence;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccination.application.CreateVaccinationService.Result;
import com.louiekury.pet.vaccination.application.CreateVaccinationService.VaccineName;
import com.louiekury.pet.vaccination.application.VaccinationPort;
import com.mongodb.MongoException;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VaccinationPersistenceAdapter implements VaccinationPort {
    private final MongoTemplate mongo;
    private final JdbcTemplate jdbc;

    private Query eligible(String petId, String caregiverId) {
        return Query.query(Criteria.where("_id").is(petId).and("caregiverId").is(caregiverId)
            .and("deletedAt").is(null));
    }

    @Override
    public boolean isOwnedPet(String petId, String caregiverId) {
        try { return mongo.exists(eligible(petId, caregiverId), "pets"); }
        catch (DataAccessException | MongoException e) { throw new BusinessException(ErrorCode.PET_QUERY_UNAVAILABLE, e); }
    }

    @Override
    public Optional<VaccineName> findVaccineName(String id) {
        try {
            return jdbc.query("SELECT uuid, name FROM vaccine WHERE uuid = ?",
                (row, index) -> new VaccineName(row.getString("uuid"), row.getString("name")), id)
                .stream().findFirst();
        } catch (DataAccessException e) { throw new BusinessException(ErrorCode.VACCINE_QUERY_UNAVAILABLE, e); }
    }

    @Override
    public boolean saveIfOwned(Result result, String caregiverId) {
        Document data = new Document("vaccinationId", result.vaccinationId())
            .append("vaccineNameId", result.vaccineName().vaccineNameId())
            .append("vaccineName", result.vaccineName().name())
            .append("vaccinatedOn", result.vaccinatedOn().toString())
            .append("nextVaccinationOn", result.nextVaccinationOn() == null ? null : result.nextVaccinationOn().toString())
            .append("doseNumber", result.doseNumber()).append("hospitalName", result.hospitalName())
            .append("memo", result.memo()).append("createdAt", Date.from(result.createdAt()))
            .append("updatedAt", Date.from(result.updatedAt())).append("deletedAt", null);
        try {
            return mongo.updateFirst(eligible(result.petId(), caregiverId), new Update().push("vaccinations", data), "pets")
                .getMatchedCount() == 1;
        } catch (DataAccessException | MongoException e) { throw new BusinessException(ErrorCode.PERSISTENCE_UNAVAILABLE, e); }
    }
}
