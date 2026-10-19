package com.connectchat.service;

import com.connectchat.dto.TokenResponse;
import com.connectchat.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;
    private final String issuer;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${connectchat.jwt.access-token-ttl:PT15M}") Duration tokenTtl,
            @Value("${connectchat.jwt.issuer:https://connectchat.local}") String issuer) {
        if (tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("A validade do JWT deve ser positiva");
        }
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
        this.issuer = issuer;
    }

    public TokenResponse criarToken(UsuarioPrincipal usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(agora)
                .expiresAt(agora.plus(tokenTtl))
                .subject(usuario.email())
                .claim("usuarioId", usuario.id())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", tokenTtl.toSeconds());
    }
}
