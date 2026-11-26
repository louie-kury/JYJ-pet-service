package com.louiekury.pet.pet.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class PetWebConfig implements WebMvcConfigurer {

    private final PetRegistrationAuthInterceptor registrationAuth;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(registrationAuth).addPathPatterns("/api/pets", "/api/pets/*", "/api/pets/*/vaccinations");
    }
}
