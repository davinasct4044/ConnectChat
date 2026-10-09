package com.connectchat.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class Usuario {
    @NotBlank
    private String nome;
    @Min(1)
    private int idade;

    public String getNome() {
        return nome;
    }

    public int getIdade() {
        return idade;
    }

    public Usuario(String nome, int idade) {
        this.nome = nome;
        this.idade = idade;
    }


}
