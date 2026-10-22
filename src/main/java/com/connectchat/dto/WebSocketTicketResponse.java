package com.connectchat.dto;

public record WebSocketTicketResponse(String ticket, long expiresInSeconds) {
}
