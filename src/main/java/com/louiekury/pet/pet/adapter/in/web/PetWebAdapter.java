package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.pet.adapter.in.web.dto.request.CreatePetRequestDto;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetDetailResponseDto;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetListResponseDto;
import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.createpet.CreatePetUseCase;
import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.GetPetsResult;
import com.louiekury.pet.pet.application.getpets.GetPetsUseCase;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetWebAdapter {

    private final CreatePetUseCase createPetUseCase;
    private final GetPetsUseCase getPetsUseCase;

    public ResponseEntity<ApiResponse<PetListResponseDto>> getPets(int page, int size) {
        GetPetsResult result = getPetsUseCase.getPets(GetPetsQuery.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PetListResponseDto.from(result)));
    }

    public ResponseEntity<ApiResponse<PetDetailResponseDto>> createPet(
            CreatePetRequestDto request, String idempotencyKey) {
        PetResult result = createPetUseCase.createPet(CreatePetCommand.of(request.getName(),
            request.getSpeciesId(), request.getBreedId(), request.getProfileMediaId(),
            request.getBirth(), request.getSex(), request.getIsNeutered(), request.getWeight(),
            request.getBasicVaccinationCompleted(), idempotencyKey));
        return ResponseEntity.created(URI.create("/api/pets/" + result.getPetId()))
            .body(ApiResponse.success(PetDetailResponseDto.from(result)));
    }
}
