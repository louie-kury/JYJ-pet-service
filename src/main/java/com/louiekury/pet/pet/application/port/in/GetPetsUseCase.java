package com.louiekury.pet.pet.application.port.in;

import com.louiekury.pet.pet.application.getpets.GetPetsQuery;
import com.louiekury.pet.pet.application.getpets.GetPetsResult;

public interface GetPetsUseCase {

    GetPetsResult getPets(GetPetsQuery query);
}
