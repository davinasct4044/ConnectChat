package com.connectchat.controller;

import com.connectchat.dto.UsuarioCadastroRequest;
import com.connectchat.dto.UsuarioResponse;
import com.connectchat.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> retornarUsuariosDoBanco() {
        return usuarioService.listar();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse adicionarUsuarioNoBanco(
            @Valid @RequestBody UsuarioCadastroRequest request) {
        return usuarioService.cadastrar(request);
    }
}
