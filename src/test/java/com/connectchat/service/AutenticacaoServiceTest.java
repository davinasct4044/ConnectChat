package com.connectchat.service;

import com.connectchat.dto.LoginRequest;
import com.connectchat.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutenticacaoServiceTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final AutenticacaoService service =
            new AutenticacaoService(authenticationManager, jwtTokenService);

    @Test
    void autenticaComEmailNormalizadoEEmiteToken() {
        var principal = new UsuarioPrincipal(17L, "ana@example.com", "hash");
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
        var esperado = new com.connectchat.dto.TokenResponse("token", "Bearer", 900);
        when(jwtTokenService.criarToken(principal)).thenReturn(esperado);

        var resposta = service.login(new LoginRequest(
                " Ana@Example.com ", "senha-correta"));

        assertEquals(esperado, resposta);
        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken token
                        && token.getPrincipal().equals("ana@example.com")
                        && token.getCredentials().equals("senha-correta")));
    }

    @Test
    void credenciaisInvalidasRetornam401SemEmitirToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("ana@example.com", "errada")));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
        verifyNoInteractions(jwtTokenService);
    }
}
