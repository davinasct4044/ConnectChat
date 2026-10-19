package com.connectchat.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
