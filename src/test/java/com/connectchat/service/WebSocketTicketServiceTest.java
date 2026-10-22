package com.connectchat.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketTicketServiceTest {

    @Test
    void ticketEOpacoESoPodeSerConsumidoUmaVez() {
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofSeconds(30));

        WebSocketTicketService.IssuedTicket ticket = service.emitir(42L);

        assertEquals(30, ticket.expiresInSeconds());
        assertTrue(ticket.value().length() >= 40);
        assertEquals(42L, service.consumir(ticket.value()));
        assertNull(service.consumir(ticket.value()));
    }

    @Test
    void ticketExpiradoNaoPodeSerConsumido() throws InterruptedException {
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofMillis(1));
        String ticket = service.emitir(42L).value();
        Thread.sleep(10);

        assertNull(service.consumir(ticket));
    }

    @Test
    void ticketsDiferentesSaoGerados() {
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofSeconds(30));

        String primeiro = service.emitir(42L).value();
        String segundo = service.emitir(42L).value();

        assertNotNull(primeiro);
        assertTrue(!primeiro.equals(segundo));
    }
}
