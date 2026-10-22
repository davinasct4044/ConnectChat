package com.connectchat.security;

import com.connectchat.config.JwtConfig;
import com.connectchat.config.PasswordConfig;
import com.connectchat.config.SecurityConfig;
import com.connectchat.controller.ConversaController;
import com.connectchat.controller.MensagemController;
import com.connectchat.controller.AutenticacaoController;
import com.connectchat.controller.UsuarioController;
import com.connectchat.dto.LoginRequest;
import com.connectchat.dto.TokenResponse;
import com.connectchat.dto.UsuarioCadastroRequest;
import com.connectchat.dto.UsuarioResponse;
import com.connectchat.model.Conversa;
import com.connectchat.model.Mensagem;
import com.connectchat.model.Usuario;
import com.connectchat.repository.ConversaRepository;
import com.connectchat.repository.MensagemRepository;
import com.connectchat.repository.UsuarioRepository;
import com.connectchat.service.UsuarioService;
import com.connectchat.service.AutenticacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UsuarioController.class,
        AutenticacaoController.class,
        MensagemController.class,
        ConversaController.class
})
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class})
@TestPropertySource(properties = {
        "connectchat.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "connectchat.jwt.issuer=https://connectchat.local"
})
class RestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private MensagemRepository mensagemRepository;

    @MockitoBean
    private ConversaRepository conversaRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void exigeTokenParaListarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteListarUsuariosComTokenValido() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/usuarios")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroELoginPermanecemPublicos() throws Exception {
        when(usuarioService.cadastrar(any(UsuarioCadastroRequest.class)))
                .thenReturn(new UsuarioResponse(1L, "Ana", 20, "ana@example.com"));
        when(autenticacaoService.login(any(LoginRequest.class)))
                .thenReturn(new TokenResponse("token-de-teste", "Bearer", 900));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"idade\":20,\"email\":\"ana@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void naoPermiteLerConversaDeOutroUsuario() throws Exception {
        Conversa conversa = conversa(2L, 3L);
        when(conversaRepository.findById(9L)).thenReturn(Optional.of(conversa));

        mockMvc.perform(get("/conversas/9/mensagens")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(mensagemRepository);
    }

    @Test
    void remetenteDaMensagemVemDoTokenMesmoSeClienteEnviarOutroId() throws Exception {
        Usuario remetenteAutenticado = usuario(1L);
        Conversa conversa = conversa(1L, 2L);
        when(conversaRepository.findById(9L)).thenReturn(
                Optional.of(conversa));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(remetenteAutenticado));
        when(mensagemRepository.save(any(Mensagem.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(post("/conversas/9/mensagens")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remetenteId\":2,\"conteudo\":\"Olá\"}"))
                .andExpect(status().isCreated());

        var captor = org.mockito.ArgumentCaptor.forClass(Mensagem.class);
        verify(mensagemRepository).save(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getRemetente().getId());
    }

    @Test
    void criacaoDeConversaUsaUsuarioAutenticadoEOutroParticipanteInformado() throws Exception {
        Usuario autenticado = usuario(1L);
        Usuario outro = usuario(2L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autenticado));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outro));
        when(conversaRepository.save(any(Conversa.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(post("/conversas")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outroUsuarioId\":2}"))
                .andExpect(status().isCreated());

        var captor = org.mockito.ArgumentCaptor.forClass(Conversa.class);
        verify(conversaRepository).save(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getUsuario1().getId());
        assertEquals(Long.valueOf(2L), captor.getValue().getUsuario2().getId());
    }

    private String bearerToken(Long usuarioId) {
        Instant agora = Instant.now();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("https://connectchat.local")
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(900))
                        .subject("usuario" + usuarioId + "@example.com")
                        .claim("usuarioId", usuarioId)
                        .build())).getTokenValue();
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getNome()).thenReturn("Teste");
        when(usuario.getIdade()).thenReturn(20);
        when(usuario.getEmail()).thenReturn("teste" + id + "@example.com");
        return usuario;
    }

    private static Conversa conversa(Long usuario1Id, Long usuario2Id) {
        Conversa conversa = mock(Conversa.class);
        Usuario usuario1 = usuario(usuario1Id);
        Usuario usuario2 = usuario(usuario2Id);
        when(conversa.getId()).thenReturn(9L);
        when(conversa.getUsuario1()).thenReturn(usuario1);
        when(conversa.getUsuario2()).thenReturn(usuario2);
        return conversa;
    }
}
