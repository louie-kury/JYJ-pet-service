package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.security.AuthenticatedMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class PetRegistrationAuthInterceptor implements HandlerInterceptor {

    public static final String CAREGIVER_ID = "pet.registration.caregiverId";
    private final AuthenticatedMember authenticatedMember;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equals(request.getMethod()) || "GET".equals(request.getMethod())
                || "HEAD".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) {
            request.setAttribute(CAREGIVER_ID, authenticatedMember.require(request.getHeader("Authorization")));
        }
        return true;
    }
}
