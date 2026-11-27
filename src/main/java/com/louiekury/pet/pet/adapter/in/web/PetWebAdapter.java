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
        if (request.getName().isPresent()) {
            changes.put("name", request.getName().getValue());
        }
        if (request.getSpeciesId().isPresent()) {
            changes.put("speciesId", request.getSpeciesId().getValue());
        }
        if (request.getBreedId().isPresent()) {
            changes.put("breedId", request.getBreedId().getValue());
        }
        if (request.getProfileMediaId().isPresent()) {
            changes.put("profileMediaId", request.getProfileMediaId().getValue());
        }
        if (request.getBirth().isPresent()) {
            changes.put("birth", request.getBirth().getValue());
        }
        if (request.getSex().isPresent()) {
            changes.put("sex", request.getSex().getValue());
        }
        if (request.getIsNeutered().isPresent()) {
            changes.put("isNeutered", request.getIsNeutered().getValue());
        }
        if (request.getWeight().isPresent()) {
            changes.put("weight", request.getWeight().getValue());
        }
        if (request.getBasicVaccinationCompleted().isPresent()) {
            changes.put("basicVaccinationCompleted", request.getBasicVaccinationCompleted().getValue());
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
