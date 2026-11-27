package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.pet.application.port.in.DeletePetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetDeleteWebAdapter {
    private final DeletePetUseCase deletePetUseCase;

    public ResponseEntity<Void> deletePet(String petId, String caregiverId) {
        deletePetUseCase.delete(petId, caregiverId);
        return ResponseEntity.noContent().build();
    }
}
