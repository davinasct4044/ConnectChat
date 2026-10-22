package com.connectchat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketTicketService {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ticketTtl;

    public WebSocketTicketService(
            @Value("${connectchat.websocket.ticket-ttl:PT30S}") Duration ticketTtl) {
        if (ticketTtl.isZero() || ticketTtl.isNegative()
                || ticketTtl.compareTo(Duration.ofMinutes(1)) > 0) {
            throw new IllegalArgumentException(
                    "A validade do ticket deve ser positiva e de no máximo 1 minuto");
        }
        this.ticketTtl = ticketTtl;
    }

    public IssuedTicket emitir(Long usuarioId) {
        Instant agora = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(agora));

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiracao = agora.plus(ticketTtl);
        tickets.put(valor, new Ticket(usuarioId, expiracao));
        return new IssuedTicket(valor, ticketTtl.toSeconds());
    }

    /** Consome o ticket de modo atômico: uma segunda tentativa sempre falha. */
    public Long consumir(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        Ticket ticket = tickets.remove(valor);
        if (ticket == null || !ticket.expiresAt().isAfter(Instant.now())) {
            return null;
        }
        return ticket.usuarioId();
    }

    public record IssuedTicket(String value, long expiresInSeconds) {
    }

    private record Ticket(Long usuarioId, Instant expiresAt) {
    }
}
