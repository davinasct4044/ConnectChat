package com.connectchat.controller;

import com.connectchat.model.Usuario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class UsuarioController {
    private ArrayList<Usuario> usuarios = new ArrayList<>();

    public UsuarioController() {
        Usuario usuario = new Usuario("Davi", 19);
        addToUsuarios(usuario);
    }

    public void addToUsuarios(Usuario usuario) {
        usuarios.add(usuario);
    }
    public ArrayList<Usuario> getUsuarios() {
        return usuarios;
    }

    @GetMapping("/usuarios")
    public ArrayList<Usuario> retornarUsuarios () {
        return getUsuarios();
    }

    @PostMapping("/usuarios")
    public void adicionarUsuarioALista(@RequestBody Usuario usuario) {
        addToUsuarios(usuario);
    }
}
