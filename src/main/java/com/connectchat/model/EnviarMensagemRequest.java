package com.connectchat.model;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensagemRequest(@NotBlank String conteudo) {
}
