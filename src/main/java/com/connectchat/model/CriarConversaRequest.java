package com.connectchat.model;

import jakarta.validation.constraints.NotNull;

public record CriarConversaRequest(@NotNull Long outroUsuarioId) {
}
