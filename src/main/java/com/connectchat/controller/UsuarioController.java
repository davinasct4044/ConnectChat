package com.connectchat.controller;

import com.connectchat.model.Usuario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {
    Usuario usuario = new Usuario("Davi", 19);

    @GetMapping("/usuarios")
    public Usuario retornarUsuarios () {
        return usuario;
    }


}
