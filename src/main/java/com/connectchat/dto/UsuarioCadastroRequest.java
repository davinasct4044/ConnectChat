package com.connectchat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroRequest(
        @NotBlank String nome,
        @Min(0) int idade,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String senha
) {
}
