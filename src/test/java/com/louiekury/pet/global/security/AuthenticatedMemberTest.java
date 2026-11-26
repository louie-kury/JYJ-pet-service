package com.louiekury.pet.global.security;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AuthenticatedMemberTest {
    private static RSAKey key;
    private static HttpServer server;
    private static AuthenticatedMember verifier;

    @BeforeAll
    static void start() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] bytes = ("{\"keys\":[" + key.toPublicJWK().toJSONString() + "]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.createContext("/unavailable", exchange -> { exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start();
        verifier = new AuthenticatedMember("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks", "auth-service", "louie-services");
    }

    @AfterAll
    static void stop() { if (server != null) server.stop(0); }

    private String token(String issuer, String audience, String subject, Instant expires, RSAKey signingKey) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(subject);
        if (expires != null) claims.expirationTime(Date.from(expires));
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims.build());
        jwt.sign(new RSASSASigner(signingKey));
        return "Bearer " + jwt.serialize();
    }

    private void rejects(String authorization) {
        assertThatThrownBy(() -> verifier.require(authorization)).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test void acceptsValidSignedMemberId() throws Exception {
        assertThat(verifier.require(token("auth-service", "louie-services", "member-1", Instant.now().plusSeconds(300), key)))
            .isEqualTo("member-1");
    }
    @Test void rejectsMissingAndInvalidToken() { rejects(null); rejects("Bearer nonsense"); }
    @Test void rejectsExpired() throws Exception { rejects(token("auth-service", "louie-services", "member-1", Instant.now().minusSeconds(120), key)); }
    @Test void rejectsWrongIssuer() throws Exception { rejects(token("other", "louie-services", "member-1", Instant.now().plusSeconds(300), key)); }
    @Test void rejectsWrongAudience() throws Exception { rejects(token("auth-service", "other", "member-1", Instant.now().plusSeconds(300), key)); }
    @Test void rejectsMissingExpiry() throws Exception { rejects(token("auth-service", "louie-services", "member-1", null, key)); }
    @Test void rejectsMissingSubject() throws Exception { rejects(token("auth-service", "louie-services", null, Instant.now().plusSeconds(300), key)); }
    @Test void rejectsForgedSignature() throws Exception { rejects(token("auth-service", "louie-services", "member-1", Instant.now().plusSeconds(300), new RSAKeyGenerator(2048).generate())); }
    @Test void reportsUnavailableKeyServerSeparately() throws Exception {
        var unavailable = new AuthenticatedMember("http://127.0.0.1:" + server.getAddress().getPort() + "/unavailable", "auth-service", "louie-services");
        String authorization = token("auth-service", "louie-services", "member-1", Instant.now().plusSeconds(300), key);
        assertThatThrownBy(() -> unavailable.require(authorization)).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_SERVICE_UNAVAILABLE));
    }
}
