package com.connectchat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
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

    @Column(unique = true)
    private String email;

    private String senhaHash;

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

    public Usuario(String nome, int idade, String email, String senhaHash) {
        this.nome = nome;
        this.idade = idade;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public String getEmail() {
        return email;
    }


}
