package com.connectchat.security;

import com.connectchat.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByEmail(emailNormalizado)
                .map(usuario -> new UsuarioPrincipal(
                        usuario.getId(), emailNormalizado, usuario.senhaHashInterno()))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}
