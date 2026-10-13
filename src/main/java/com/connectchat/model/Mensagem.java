package com.connectchat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String conteudo;

    @ManyToOne
    @JoinColumn(name = "remetente_id")
    private Usuario remetente;

    @ManyToOne
    @JoinColumn(name = "conversa_id")
    private Conversa conversa;

    private LocalDateTime dataHora;

    protected Mensagem() {
    }

    public Mensagem(String conteudo, Usuario remetente, Conversa conversa) {
        this.conteudo = conteudo;
        this.remetente = remetente;
        this.conversa = conversa;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getConteudo() {
        return conteudo;
    }

    public Usuario getRemetente() {
        return remetente;
    }

    public Conversa getConversa() {
        return conversa;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}