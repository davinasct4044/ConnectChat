package com.connectchat.controller;

import com.connectchat.dto.LoginRequest;
import com.connectchat.dto.TokenResponse;
import com.connectchat.dto.WebSocketTicketResponse;
import com.connectchat.service.AutenticacaoService;
import com.connectchat.service.WebSocketTicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final WebSocketTicketService webSocketTicketService;

    public AutenticacaoController(
            AutenticacaoService autenticacaoService,
            WebSocketTicketService webSocketTicketService) {
        this.autenticacaoService = autenticacaoService;
        this.webSocketTicketService = webSocketTicketService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.login(request);
    }

    @PostMapping("/ws-ticket")
    public WebSocketTicketResponse emitirTicketWebSocket(
            @AuthenticationPrincipal Jwt jwt) {
        Number usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token sem identificação de usuário");
        }
        WebSocketTicketService.IssuedTicket ticket =
                webSocketTicketService.emitir(usuarioId.longValue());
        return new WebSocketTicketResponse(ticket.value(), ticket.expiresInSeconds());
    }
}
