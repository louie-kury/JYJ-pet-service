package com.louiekury.pet.pet.application.port.out;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.PetListPage;

public interface GetPetsPort {

    PetListPage getPets(GetPetsQuery query);
}
