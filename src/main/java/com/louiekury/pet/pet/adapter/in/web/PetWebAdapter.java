package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.pet.application.getpet.GetPetService;
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
import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdatePetRequest;
import com.louiekury.pet.pet.application.updatepet.UpdatePetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetWebAdapter {

    private final CreatePetUseCase createPetUseCase;
    private final GetPetsUseCase getPetsUseCase;
    private final UpdatePetUseCase updatePetUseCase;
    private final GetPetService getPetService;

    public ResponseEntity<ApiResponse<PetDetailResponseDto>> getPet(String id, String caregiverId) {
        return ResponseEntity.ok(ApiResponse.success(PetDetailResponseDto.from(getPetService.getPet(id, caregiverId))));
    }

    public ResponseEntity<ApiResponse<PetDetailResponseDto>> updatePet(String id, UpdatePetRequest request) {
        PetResult result = updatePetUseCase.updatePet(id, request.getChanges());
        return ResponseEntity.ok(ApiResponse.success(PetDetailResponseDto.from(result)));
    }

    public ResponseEntity<ApiResponse<PetListResponseDto>> getPets(int page, int size, String caregiverId) {
        GetPetsResult result = getPetsUseCase.getPets(GetPetsQuery.of(page, size, caregiverId));
        return ResponseEntity.ok(ApiResponse.success(PetListResponseDto.from(result)));
    }

    public ResponseEntity<ApiResponse<PetDetailResponseDto>> createPet(
            CreatePetRequestDto request, String idempotencyKey, String caregiverId) {
        PetResult result = createPetUseCase.createPet(CreatePetCommand.of(request.getName(),
            request.getSpeciesId(), request.getBreedId(), request.getProfileMediaId(),
            request.getBirth(), request.getSex(), request.getIsNeutered(), request.getWeight(),
            request.getBasicVaccinationCompleted(), idempotencyKey, caregiverId));
        return ResponseEntity.created(URI.create("/api/pets/" + result.getPetId()))
            .body(ApiResponse.success(PetDetailResponseDto.from(result)));
    }
}
