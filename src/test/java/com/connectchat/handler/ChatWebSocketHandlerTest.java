package com.connectchat.handler;

import com.connectchat.dto.MensagemWebSocketRequest;
import com.connectchat.model.Conversa;
import com.connectchat.model.Mensagem;
import com.connectchat.model.Usuario;
import com.connectchat.repository.ConversaRepository;
import com.connectchat.repository.MensagemRepository;
import com.connectchat.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketHandlerTest {

    @Test
    void remetenteDaMensagemVemDaSessaoMesmoComIdFalsificadoNoJson() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ConversaRepository conversaRepository = mock(ConversaRepository.class);
        MensagemRepository mensagemRepository = mock(MensagemRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ExposedChatWebSocketHandler handler = new ExposedChatWebSocketHandler(
                objectMapper, usuarioRepository, conversaRepository, mensagemRepository);
        WebSocketSession session = sessionAs(1L);
        Usuario authenticatedUser = usuario(1L);
        Conversa conversa = conversa(10L, 1L, 2L);
        Mensagem savedMessage = mensagem(100L, "Oi", authenticatedUser);

        when(objectMapper.readValue("payload", Map.class))
                .thenReturn(Map.of("tipo", "mensagem"));
        when(objectMapper.readValue("payload", MensagemWebSocketRequest.class))
                .thenReturn(new MensagemWebSocketRequest(10L, 99L, "Oi"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(authenticatedUser));
        when(mensagemRepository.save(any(Mensagem.class))).thenReturn(savedMessage);

        handler.receive(session, "payload");

        var captor = org.mockito.ArgumentCaptor.forClass(Mensagem.class);
        verify(mensagemRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getRemetente().getId());
    }

    @Test
    void bloqueiaEnvioQuandoUsuarioDaSessaoNaoParticipaDaConversa() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ConversaRepository conversaRepository = mock(ConversaRepository.class);
        MensagemRepository mensagemRepository = mock(MensagemRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ExposedChatWebSocketHandler handler = new ExposedChatWebSocketHandler(
                objectMapper, usuarioRepository, conversaRepository, mensagemRepository);

        when(objectMapper.readValue("payload", Map.class))
                .thenReturn(Map.of("tipo", "mensagem"));
        when(objectMapper.readValue("payload", MensagemWebSocketRequest.class))
                .thenReturn(new MensagemWebSocketRequest(10L, 1L, "Oi"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        Conversa conversaNaoParticipante = conversa(10L, 2L, 3L);
        when(conversaRepository.findById(10L))
                .thenReturn(Optional.of(conversaNaoParticipante));
        Usuario usuarioDaSessao = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioDaSessao));

        handler.receive(sessionAs(1L), "payload");

        verify(mensagemRepository, never()).save(any(Mensagem.class));
    }

    @Test
    void historicoSoECarregadoPorParticipanteDaConversa() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ConversaRepository conversaRepository = mock(ConversaRepository.class);
        MensagemRepository mensagemRepository = mock(MensagemRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ExposedChatWebSocketHandler handler = new ExposedChatWebSocketHandler(
                objectMapper, usuarioRepository, conversaRepository, mensagemRepository);
        WebSocketSession session = sessionAs(1L);
        when(objectMapper.readValue("payload", Map.class))
                .thenReturn(Map.of("tipo", "abrir_conversa"));
        when(objectMapper.readValue("payload", com.connectchat.dto.AbrirConversaRequest.class))
                .thenReturn(new com.connectchat.dto.AbrirConversaRequest(10L));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        Conversa conversaDoParticipante = conversa(10L, 1L, 2L);
        when(conversaRepository.findById(10L))
                .thenReturn(Optional.of(conversaDoParticipante));
        when(mensagemRepository.findByConversaIdOrderByDataHoraAsc(10L))
                .thenReturn(List.of());

        handler.receive(session, "payload");

        verify(mensagemRepository).findByConversaIdOrderByDataHoraAsc(10L);
        assertEquals(10L, session.getAttributes().get("conversaId"));
    }

    private static WebSocketSession sessionAs(Long usuarioId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("usuarioId", usuarioId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getNome()).thenReturn("Usuário " + id);
        return usuario;
    }

    private static Conversa conversa(Long id, Long usuario1Id, Long usuario2Id) {
        Conversa conversa = mock(Conversa.class);
        Usuario usuario1 = usuario(usuario1Id);
        Usuario usuario2 = usuario(usuario2Id);
        when(conversa.getId()).thenReturn(id);
        when(conversa.getUsuario1()).thenReturn(usuario1);
        when(conversa.getUsuario2()).thenReturn(usuario2);
        return conversa;
    }

    private static Mensagem mensagem(Long id, String conteudo, Usuario remetente) {
        Mensagem mensagem = mock(Mensagem.class);
        when(mensagem.getId()).thenReturn(id);
        when(mensagem.getConteudo()).thenReturn(conteudo);
        when(mensagem.getRemetente()).thenReturn(remetente);
        when(mensagem.getDataHora()).thenReturn(LocalDateTime.now());
        return mensagem;
    }

    private static class ExposedChatWebSocketHandler extends ChatWebSocketHandler {
        ExposedChatWebSocketHandler(
                ObjectMapper objectMapper,
                UsuarioRepository usuarioRepository,
                ConversaRepository conversaRepository,
                MensagemRepository mensagemRepository) {
            super(objectMapper, usuarioRepository, conversaRepository, mensagemRepository);
        }

        void receive(WebSocketSession session, String payload) throws Exception {
            handleTextMessage(session, new TextMessage(payload));
        }
    }
}
