package com.connectchat.websocket;

import com.connectchat.service.WebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketTicketHandshakeInterceptorTest {

    @Test
    void handshakeAutenticadoRecebeIdEConsomeTicket() throws Exception {
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofSeconds(30));
        WebSocketTicketService.IssuedTicket ticket = service.emitir(7L);
        WebSocketTicketHandshakeInterceptor interceptor =
                new WebSocketTicketHandshakeInterceptor(service);
        ServerHttpRequest request = request("ws://localhost/chat?ticket=" + ticket.value());
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(
                request, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes));
        assertEquals(7L, attributes.get("usuarioId"));
        assertFalse(interceptor.beforeHandshake(
                request, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), new HashMap<>()));
    }

    @Test
    void handshakeSemTicketOuComTicketDuplicadoFalha() throws Exception {
        WebSocketTicketHandshakeInterceptor interceptor =
                new WebSocketTicketHandshakeInterceptor(
                        new WebSocketTicketService(Duration.ofSeconds(30)));
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);

        assertFalse(interceptor.beforeHandshake(
                request("ws://localhost/chat"), response, handler, new HashMap<>()));
        assertFalse(interceptor.beforeHandshake(
                request("ws://localhost/chat?ticket=a&ticket=b"), response, handler,
                new HashMap<>()));
    }

    private static ServerHttpRequest request(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        return request;
    }
}
