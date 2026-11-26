package com.louiekury.pet.global.security;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedMember {
    private final String jwksUri;
    private final String issuer;
    private final String audience;
    private volatile NimbusJwtDecoder decoder;

    public AuthenticatedMember(@Value("${pet.auth.jwks-uri:}") String jwksUri,
            @Value("${pet.auth.issuer:}") String issuer, @Value("${pet.auth.audience:}") String audience) {
        this.jwksUri = jwksUri;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String require(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if (jwksUri.isBlank() || issuer.isBlank() || audience.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }
        try {
            Jwt jwt = decoder().decode(authorization.substring(7));
            return jwt.getSubject();
        } catch (BadJwtException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        } catch (JwtException exception) {
            throw new BusinessException(ErrorCode.AUTH_SERVICE_UNAVAILABLE, exception);
        }
    }

    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder current = decoder;
        if (current == null) {
            synchronized (this) {
                current = decoder;
                if (current == null) {
                    current = NimbusJwtDecoder.withJwkSetUri(jwksUri).jwsAlgorithm(SignatureAlgorithm.RS256).build();
                    OAuth2TokenValidator<Jwt> claims = jwt -> {
                        List<String> audiences = jwt.getAudience();
                        if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()
                                || audiences == null || !audiences.contains(audience)) {
                            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                        }
                        return OAuth2TokenValidatorResult.success();
                    };
                    current.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), claims));
                    decoder = current;
                }
            }
        }
        return current;
    }
}
