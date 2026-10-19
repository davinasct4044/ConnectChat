package com.connectchat.service;

import com.connectchat.dto.UsuarioCadastroRequest;
import com.connectchat.model.Usuario;
import com.connectchat.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UsuarioService service = new UsuarioService(repository, passwordEncoder);

    @Test
    void cadastraComEmailNormalizadoESenhaEmHash() {
        when(repository.existsByEmail("aluna@example.com")).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            assertEquals("aluna@example.com", usuario.getEmail());
            assertTrue(passwordEncoder.matches("senha-segura", senhaHash(usuario)));
            assertFalse(senhaHash(usuario).equals("senha-segura"));
            return usuario;
        });

        var resposta = service.cadastrar(new UsuarioCadastroRequest(
                "Aluna", 20, " Aluna@Example.com ", "senha-segura"));

        assertEquals("aluna@example.com", resposta.email());
        assertFalse(resposta.toString().contains("senha-segura"));
    }

    @Test
    void rejeitaEmailJaCadastrado() {
        when(repository.existsByEmail("aluna@example.com")).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.cadastrar(new UsuarioCadastroRequest(
                        "Aluna", 20, "aluna@example.com", "senha-segura")));

        assertEquals(409, erro.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    private static String senhaHash(Usuario usuario) throws Exception {
        var campo = Usuario.class.getDeclaredField("senhaHash");
        campo.setAccessible(true);
        return (String) campo.get(usuario);
    }
}
