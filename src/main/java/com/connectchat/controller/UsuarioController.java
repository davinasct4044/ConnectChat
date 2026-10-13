package com.connectchat.controller;

import com.connectchat.model.Usuario;
import com.connectchat.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class UsuarioController {
    private final UsuarioRepository usuarioRepository;


    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/usuarios")
    public List<Usuario> retornarUsuariosDoBanco () {
        return usuarioRepository.findAll();
    }

    @PostMapping("/usuarios")
    public Usuario adicionarUsuarioNoBanco(@Valid @RequestBody Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}
