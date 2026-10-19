package com.connectchat.service;

import com.connectchat.dto.UsuarioCadastroRequest;
import com.connectchat.dto.UsuarioResponse;
import com.connectchat.model.Usuario;
import com.connectchat.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw emailJaCadastrado();
        }

        Usuario usuario = new Usuario(
                request.nome().trim(),
                request.idade(),
                email,
                passwordEncoder.encode(request.senha())
        );

        try {
            return paraResponse(usuarioRepository.save(usuario));
        } catch (DataIntegrityViolationException erro) {
            // A restrição única do banco também cobre cadastros concorrentes.
            throw emailJaCadastrado();
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioService::paraResponse)
                .toList();
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(), usuario.getNome(), usuario.getIdade(), usuario.getEmail());
    }

    private static ResponseStatusException emailJaCadastrado() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "Este e-mail já está cadastrado");
    }
}
