package com.connectchat.model;

public record EnviarMensagemRequest(
        Long remetenteId,
        String conteudo
) {

}
