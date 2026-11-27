package com.louiekury.pet.pet.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePetRequest {
    private UpdateField<@NotBlank(message = "반려동물 이름은 필수입니다.") String> name = UpdateField.absent();
    private UpdateField<@NotBlank(message = "반려동물 종은 필수입니다.") String> speciesId = UpdateField.absent();
    private UpdateField<String> breedId = UpdateField.absent();
    private UpdateField<String> profileMediaId = UpdateField.absent();
    private UpdateField<LocalDate> birth = UpdateField.absent();
    private UpdateField<@NotBlank(message = "반려동물 성별은 필수입니다.") String> sex = UpdateField.absent();
    private UpdateField<Boolean> isNeutered = UpdateField.absent();
    private UpdateField<BigDecimal> weight = UpdateField.absent();
    private UpdateField<Boolean> basicVaccinationCompleted = UpdateField.absent();
}
