package com.connectchat.controller;

import com.connectchat.model.Conversa;
import com.connectchat.model.CriarConversaRequest;
import com.connectchat.model.Usuario;
import com.connectchat.repository.ConversaRepository;
import com.connectchat.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
    public Conversa criar(
            @Valid @RequestBody CriarConversaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Number claimUsuarioId = jwt.getClaim("usuarioId");
        if (claimUsuarioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token sem identificação de usuário");
        }
        Long usuarioId = claimUsuarioId.longValue();

        if (usuarioId.equals(request.outroUsuarioId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Não é possível criar conversa consigo mesmo");
        }

        Usuario usuarioAutenticado = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado"));
        Usuario outroUsuario = usuarioRepository.findById(request.outroUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Outro usuário não encontrado"));

        return conversaRepository.save(new Conversa(usuarioAutenticado, outroUsuario));
    }
}
