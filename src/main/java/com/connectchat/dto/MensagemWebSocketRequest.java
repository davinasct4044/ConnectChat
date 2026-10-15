package com.connectchat.dto;

public record MensagemWebSocketRequest(
        Long conversaId,
        Long remetenteId,
        String conteudo
) {
}
