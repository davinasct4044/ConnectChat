package com.connectchat.controller;

import com.connectchat.model.Conversa;
import com.connectchat.model.EnviarMensagemRequest;
import com.connectchat.model.Mensagem;
import com.connectchat.model.Usuario;
import com.connectchat.repository.ConversaRepository;
import com.connectchat.repository.MensagemRepository;
import com.connectchat.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
            @PathVariable Long conversaId,
            @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = usuarioId(jwt);
        obterConversaDoParticipante(conversaId, usuarioId);
        return mensagemRepository.findByConversaIdOrderByDataHoraAsc(conversaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mensagem enviar(
            @PathVariable Long conversaId,
            @Valid @RequestBody EnviarMensagemRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = usuarioId(jwt);
        Conversa conversa = obterConversaDoParticipante(conversaId, usuarioId);
        Usuario remetente = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado"));

        return mensagemRepository.save(
                new Mensagem(request.conteudo(), remetente, conversa));
    }

    private Conversa obterConversaDoParticipante(Long conversaId, Long usuarioId) {
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conversa não encontrada"));

        boolean participa = conversa.getUsuario1().getId().equals(usuarioId)
                || conversa.getUsuario2().getId().equals(usuarioId);
        if (!participa) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não participa desta conversa");
        }
        return conversa;
    }

    private static Long usuarioId(Jwt jwt) {
        Number usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token sem identificação de usuário");
        }
        return usuarioId.longValue();
    }
}
