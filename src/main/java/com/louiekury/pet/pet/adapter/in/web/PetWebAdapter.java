package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.pet.adapter.in.web.dto.request.CreatePetRequestDto;
import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdatePetRequest;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetDetailResponseDto;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetListResponseDto;
import com.louiekury.pet.pet.application.createpet.CreatePetCommand;
import com.louiekury.pet.pet.application.createpet.PetResult;
import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.GetPetsResult;
import com.louiekury.pet.pet.application.port.in.CreatePetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetUseCase;
import com.louiekury.pet.pet.application.port.in.GetPetsUseCase;
import com.louiekury.pet.pet.application.port.in.UpdatePetUseCase;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetWebAdapter {

    private final CreatePetUseCase createPetUseCase;
    private final GetPetsUseCase getPetsUseCase;
    private final UpdatePetUseCase updatePetUseCase;
    private final GetPetUseCase getPetService;

    public ResponseEntity<ApiResponse<PetDetailResponseDto>> getPet(String id, String caregiverId) {
        return ResponseEntity.ok(ApiResponse.success(PetDetailResponseDto.from(getPetService.getPet(id, caregiverId))));
    }

    public ResponseEntity<ApiResponse<PetDetailResponseDto>> updatePet(
            String id, UpdatePetRequest request, String caregiverId) {
        PetResult result = updatePetUseCase.updatePet(id, toChanges(request), caregiverId);
        return ResponseEntity.ok(ApiResponse.success(PetDetailResponseDto.from(result)));
    }

    private Map<String, Object> toChanges(UpdatePetRequest request) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (request.hasName()) {
            changes.put("name", request.getName());
        }
        if (request.hasSpeciesId()) {
            changes.put("speciesId", request.getSpeciesId());
        }
        if (request.hasBreedId()) {
            changes.put("breedId", request.getBreedId());
        }
        if (request.hasProfileMediaId()) {
            changes.put("profileMediaId", request.getProfileMediaId());
        }
        if (request.hasBirth()) {
            changes.put("birth", request.getBirth());
        }
        if (request.hasSex()) {
            changes.put("sex", request.getSex());
        }
        if (request.hasIsNeutered()) {
            changes.put("isNeutered", request.getIsNeutered());
        }
        if (request.hasWeight()) {
            changes.put("weight", request.getWeight());
        }
        if (request.hasBasicVaccinationCompleted()) {
            changes.put("basicVaccinationCompleted", request.getBasicVaccinationCompleted());
        }
        return changes;
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
