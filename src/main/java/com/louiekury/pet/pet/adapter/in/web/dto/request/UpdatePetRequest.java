package com.louiekury.pet.pet.adapter.in.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePetRequest {
    private UpdateField<String> name = UpdateField.absent();
    private UpdateField<String> speciesId = UpdateField.absent();
    private UpdateField<String> breedId = UpdateField.absent();
    private UpdateField<String> profileMediaId = UpdateField.absent();
    private UpdateField<LocalDate> birth = UpdateField.absent();
    private UpdateField<String> sex = UpdateField.absent();
    private UpdateField<Boolean> isNeutered = UpdateField.absent();
    private UpdateField<BigDecimal> weight = UpdateField.absent();
    private UpdateField<Boolean> basicVaccinationCompleted = UpdateField.absent();
}
