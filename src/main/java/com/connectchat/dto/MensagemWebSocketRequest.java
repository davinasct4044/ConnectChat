package com.connectchat.dto;

public record MensagemWebSocketRequest(
        Long conversaId,
        // Mantido para compatibilidade com clientes antigos; o servidor ignora este valor.
        Long remetenteId,
        String conteudo
) {
}
