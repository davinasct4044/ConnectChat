package com.connectchat.model;

public class Usuario {
    private String nome;
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
