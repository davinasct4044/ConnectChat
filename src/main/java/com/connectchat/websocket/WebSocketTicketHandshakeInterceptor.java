package com.connectchat.websocket;

import com.connectchat.service.WebSocketTicketService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class WebSocketTicketHandshakeInterceptor implements HandshakeInterceptor {

    private final WebSocketTicketService ticketService;

    public WebSocketTicketHandshakeInterceptor(WebSocketTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> attributes) {
        String ticket = extrairTicket(request.getURI().getRawQuery());
        Long usuarioId = ticketService.consumir(ticket);
        if (usuarioId == null) {
            return false;
        }
        attributes.put("usuarioId", usuarioId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Exception exception) {
        // Nenhum estado adicional precisa ser limpo após o handshake.
    }

    private String extrairTicket(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String encontrado = null;
        try {
            for (String parametro : query.split("&")) {
                String[] partes = parametro.split("=", 2);
                if (partes.length == 2
                        && URLDecoder.decode(partes[0], StandardCharsets.UTF_8).equals("ticket")) {
                    if (encontrado != null) {
                        return null;
                    }
                    encontrado = URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
                }
            }
        } catch (IllegalArgumentException queryMalformada) {
            return null;
        }
        return encontrado;
    }
}
