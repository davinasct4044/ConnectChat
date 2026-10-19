package com.connectchat.service;

import com.connectchat.config.JwtConfig;
import com.connectchat.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private static final String ISSUER = "https://connectchat.local";
    private final SecretKey key = new SecretKeySpec(new byte[32], "HmacSHA256");
    private final JwtConfig config = new JwtConfig();
    private final JwtEncoder encoder = config.jwtEncoder(key);
    private final JwtDecoder decoder = config.jwtDecoder(key, ISSUER);
    private final JwtTokenService service =
            new JwtTokenService(encoder, Duration.ofMinutes(15), ISSUER);

    @Test
    void criaTokenAssinadoComExpiracaoEIdentidade() {
        var resposta = service.criarToken(new UsuarioPrincipal(
                17L, "ana@example.com", "hash-nao-usado-no-token"));

        var jwt = decoder.decode(resposta.accessToken());
        assertEquals("Bearer", resposta.tokenType());
        assertEquals(900, resposta.expiresIn());
        assertEquals("ana@example.com", jwt.getSubject());
        assertEquals(Long.valueOf(17L), jwt.getClaim("usuarioId"));
        assertEquals(ISSUER, jwt.getIssuer().toString());
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
        assertFalse(resposta.accessToken().contains("hash-nao-usado-no-token"));
    }

    @Test
    void rejeitaTokenComAssinaturaAlterada() {
        String token = service.criarToken(new UsuarioPrincipal(
                17L, "ana@example.com", "hash" )).accessToken();
        String tokenAlterado = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";

        assertThrows(JwtException.class, () -> decoder.decode(tokenAlterado));
    }

    @Test
    void rejeitaTokenExpirado() {
        Instant expirado = Instant.now().minusSeconds(120);
        String tokenExpirado = encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader
                        .with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .issuedAt(expirado.minusSeconds(60))
                        .expiresAt(expirado)
                        .subject("ana@example.com")
                        .build()))
                .getTokenValue();

        assertThrows(JwtException.class, () -> decoder.decode(tokenExpirado));
    }
}
