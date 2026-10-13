package com.connectchat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Usuario {
    @NotBlank
    private String nome;
    @Min(0)
    private int idade;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String getNome() {
        return nome;
    }

    public int getIdade() {
        return idade;
    }

    protected Usuario() {

    }

    public Long getId() {
        return id;
    }

    public Usuario(String nome, int idade) {
        this.nome = nome;
        this.idade = idade;
    }


}
