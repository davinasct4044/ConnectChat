package com.connectchat.service;

import com.connectchat.dto.LoginRequest;
import com.connectchat.dto.TokenResponse;
import com.connectchat.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AutenticacaoService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        try {
            Authentication autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email, request.senha()));
            return jwtTokenService.criarToken((UsuarioPrincipal) autenticacao.getPrincipal());
        } catch (AuthenticationException erro) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
    }
}
