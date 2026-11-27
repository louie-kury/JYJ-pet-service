package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.security.AuthenticatedMember;
import com.louiekury.pet.global.web.exception.GlobalExceptionHandler;
import com.louiekury.pet.pet.application.deletepet.DeletePetService;
import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetMvcAuthConfigTest {
    @Configuration
    @EnableWebMvc
    @Import(PetWebConfig.class)
    static class Config {
        @Bean AuthenticatedMember auth() { return mock(AuthenticatedMember.class); }
        @Bean PetWebAdapter adapter() { return mock(PetWebAdapter.class); }
        @Bean PetRegistrationAuthInterceptor interceptor(AuthenticatedMember auth) { return new PetRegistrationAuthInterceptor(auth); }
        @Bean PetController petController(PetWebAdapter adapter) { return new PetController(adapter); }
        @Bean PetUpdateController updateController(PetWebAdapter adapter) { return new PetUpdateController(adapter); }
        @Bean DeletePetService deletePetService() { return mock(DeletePetService.class); }
        @Bean PetDeleteController deleteController(DeletePetService service) { return new PetDeleteController(new PetDeleteWebAdapter(service)); }
        @Bean GlobalExceptionHandler exceptionHandler() { return new GlobalExceptionHandler(); }
    }

    @Test void deleteUsesConfiguredInterceptor() throws Exception {
        try (var context = context()) {
            var auth = context.getBean(AuthenticatedMember.class);
            var service = context.getBean(DeletePetService.class);
            when(auth.require(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
            when(auth.require("Bearer valid")).thenReturn("owner-a");
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(delete("/api/pets/pet-1").header("X-User-Id", "owner-a"))
                .andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
            mvc.perform(delete("/api/pets/pet-1").header("Authorization", "Bearer valid")
                    .header("X-User-Id", "attacker"))
                .andExpect(status().isNoContent()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
            verify(service).delete("pet-1", "owner-a");
        }
    }

    @Test void patchUsesConfiguredInterceptor() throws Exception {
        try (var context = context()) {
            var auth = context.getBean(AuthenticatedMember.class);
            var adapter = context.getBean(PetWebAdapter.class);
            when(auth.require(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
            when(auth.require("Bearer valid")).thenReturn("owner-a");
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(patch("/api/pets/pet-1").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isUnauthorized());
            verifyNoInteractions(adapter);
            mvc.perform(patch("/api/pets/pet-1").header("Authorization", "Bearer valid")
                    .header("X-User-Id", "attacker").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"루이\"}"))
                .andExpect(status().isOk());
            verify(adapter).updatePet(eq("pet-1"), any(), eq("owner-a"));
            verify(auth).require("Bearer valid");
        }
    }

    @Test void detailGetAndHeadValidateOnce() throws Exception {
        try (var context = context()) {
            var auth = context.getBean(AuthenticatedMember.class);
            var adapter = context.getBean(PetWebAdapter.class);
            when(auth.require("Bearer valid")).thenReturn("owner-a");
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(get("/api/pets/pet-1").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
            verify(auth, times(1)).require("Bearer valid");
            verify(adapter, times(1)).getPet("pet-1", "owner-a");
            mvc.perform(head("/api/pets/pet-1").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
            verify(auth, times(2)).require("Bearer valid");
            verify(adapter, times(2)).getPet("pet-1", "owner-a");
        }
    }

    private AnnotationConfigWebApplicationContext context() {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        return context;
    }
}
