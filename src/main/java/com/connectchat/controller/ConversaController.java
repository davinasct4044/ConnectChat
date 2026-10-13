package com.connectchat.controller;

import com.connectchat.model.Conversa;
import com.connectchat.model.CriarConversaRequest;
import com.connectchat.model.Usuario;
import com.connectchat.repository.ConversaRepository;
import com.connectchat.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/conversas")
public class ConversaController {

    private final ConversaRepository conversaRepository;
    private final UsuarioRepository usuarioRepository;

    public ConversaController(
            ConversaRepository conversaRepository,
            UsuarioRepository usuarioRepository) {
        this.conversaRepository = conversaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Conversa criar(@RequestBody CriarConversaRequest request) {
        Usuario usuario1 = usuarioRepository.findById(request.usuario1Id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário 1 não encontrado"));

        Usuario usuario2 = usuarioRepository.findById(request.usuario2Id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário 2 não encontrado"));

        Conversa conversa = new Conversa(usuario1, usuario2);

        return conversaRepository.save(conversa);
    }
}