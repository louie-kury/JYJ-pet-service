package com.louiekury.pet.pet.application.port.in;

public interface DeletePetUseCase {
    void delete(String id, String caregiverId);
}
