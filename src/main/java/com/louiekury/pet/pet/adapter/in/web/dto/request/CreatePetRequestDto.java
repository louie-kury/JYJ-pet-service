package com.louiekury.pet.pet.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePetRequestDto {
    @NotBlank(message = "반려동물 이름은 필수입니다.")
    private String name;
    @NotBlank(message = "반려동물 종은 필수입니다.")
    private String speciesId;
    private String breedId;
    private String profileMediaId;
    private LocalDate birth;
    @NotBlank(message = "반려동물 성별은 필수입니다.")
    private String sex;
    private Boolean isNeutered;
    private BigDecimal weight;
    private Boolean basicVaccinationCompleted;

}
