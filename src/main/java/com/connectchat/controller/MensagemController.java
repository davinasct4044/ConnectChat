package com.connectchat.controller;

import com.connectchat.model.*;
import com.connectchat.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/conversas/{conversaId}/mensagens")
public class MensagemController {

    private final MensagemRepository mensagemRepository;
    private final ConversaRepository conversaRepository;
    private final UsuarioRepository usuarioRepository;

    public MensagemController(
            MensagemRepository mensagemRepository,
            ConversaRepository conversaRepository,
            UsuarioRepository usuarioRepository) {
        this.mensagemRepository = mensagemRepository;
        this.conversaRepository = conversaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Mensagem> listarMensagens(
            @PathVariable Long conversaId) {

        return mensagemRepository
                .findByConversaIdOrderByDataHoraAsc(conversaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mensagem enviar(
            @PathVariable Long conversaId,
            @RequestBody EnviarMensagemRequest request) {

        var conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conversa não encontrada"));

        var remetente = usuarioRepository.findById(request.remetenteId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Remetente não encontrado"));

        boolean participa =
                conversa.getUsuario1().getId().equals(remetente.getId())
                        || conversa.getUsuario2().getId().equals(remetente.getId());

        if (!participa) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "O remetente não participa desta conversa");
        }

        if (request.conteudo() == null || request.conteudo().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O conteúdo não pode estar vazio");
        }

        Mensagem mensagem = new Mensagem(
                request.conteudo(), remetente, conversa);

        return mensagemRepository.save(mensagem);
    }
}