package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.pet.adapter.in.web.dto.request.UpdatePetRequest;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetDetailResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetUpdateController {
    private final PetWebAdapter petWebAdapter;

    @PatchMapping(value = "/{petId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PetDetailResponseDto>> updatePet(
            @PathVariable String petId, @Valid @RequestBody UpdatePetRequest request,
            @RequestAttribute(PetRegistrationAuthInterceptor.CAREGIVER_ID) String caregiverId) {
        return petWebAdapter.updatePet(petId, request, caregiverId);
    }
}
