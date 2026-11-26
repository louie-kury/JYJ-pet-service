package com.louiekury.pet.vaccination.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.pet.adapter.in.web.PetRegistrationAuthInterceptor;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.louiekury.pet.vaccination.application.CreateVaccinationService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequiredArgsConstructor
public class VaccinationController {
    private final CreateVaccinationService service;
    private static final Set<String> FIELDS = Set.of("vaccineNameId", "vaccinatedOn", "nextVaccinationOn",
        "doseNumber", "hospitalName", "memo");

    @PostMapping(value = "/api/pets/{petId}/vaccinations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CreateVaccinationService.Result>> create(@PathVariable String petId,
            @RequestAttribute(PetRegistrationAuthInterceptor.CAREGIVER_ID) String caregiverId,
            @RequestBody JsonNode body) {
        if (!body.isObject()) throw invalid();
        body.propertyNames().forEach(name -> { if (!FIELDS.contains(name)) throw invalid(); });
        JsonNode dose = body.get("doseNumber");
        if (dose != null && !dose.isNull() && (!dose.isIntegralNumber() || !dose.canConvertToInt())) throw invalid();
        var request = new CreateVaccinationService.Request(text(body, "vaccineNameId"), date(body, "vaccinatedOn"),
            date(body, "nextVaccinationOn"), dose == null || dose.isNull() ? null : dose.intValue(),
            text(body, "hospitalName"), text(body, "memo"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(petId, caregiverId, request)));
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) return null;
        if (!node.isString()) throw invalid();
        return node.stringValue();
    }
    private static LocalDate date(JsonNode body, String field) {
        String value = text(body, field);
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { throw invalid(); }
    }
    private static BusinessException invalid() { return new BusinessException(ErrorCode.VALIDATION_ERROR); }
}
