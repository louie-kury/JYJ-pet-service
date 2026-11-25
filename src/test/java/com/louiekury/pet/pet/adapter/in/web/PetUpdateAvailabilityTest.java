package com.louiekury.pet.pet.adapter.in.web;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetUpdateAvailabilityTest {
    @Configuration
    @EnableWebMvc
    static class TestWebConfig {
        @Bean PetWebAdapter petWebAdapter() { return mock(PetWebAdapter.class); }
    }

    @Test
    void patchIsUnavailableByDefault() throws Exception {
        assertPatchAvailability(null, null, false);
    }

    @Test
    void patchIsUnavailableInDevWithoutExplicitOptIn() throws Exception {
        assertPatchAvailability("dev", null, false);
    }

    @Test
    void patchIsUnavailableInDevWhenExplicitlyDisabled() throws Exception {
        assertPatchAvailability("dev", "false", false);
    }

    @Test
    void patchIsUnavailableOutsideDevEvenIfEnabled() throws Exception {
        assertPatchAvailability("prod", "true", false);
    }

    @Test
    void patchIsAvailableOnlyWithDevProfileAndOptIn() throws Exception {
        assertPatchAvailability("dev", "true", true);
    }

    private void assertPatchAvailability(String profile, String enabled, boolean expected) throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            if (profile != null) context.getEnvironment().setActiveProfiles(profile);
            if (enabled != null) context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("pet.anonymous-update.enabled", enabled)));
            context.register(TestWebConfig.class, PetUpdateController.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            var request = mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"루이\"}"));
            if (expected) {
                request.andExpect(status().isOk());
                verify(context.getBean(PetWebAdapter.class)).updatePet(eq("pet-1"), any());
            } else {
                request.andExpect(status().isNotFound());
                verifyNoInteractions(context.getBean(PetWebAdapter.class));
            }
        }
    }
}
