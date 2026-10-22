package com.connectchat.handler;

import com.connectchat.dto.AbrirConversaRequest;
import com.connectchat.dto.MensagemWebSocketRequest;
import com.connectchat.model.Conversa;
import com.connectchat.model.Mensagem;
import com.connectchat.model.Usuario;
import com.connectchat.repository.ConversaRepository;
import com.connectchat.repository.MensagemRepository;
import com.connectchat.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessoes =
            new CopyOnWriteArraySet<>();

    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;
    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;

    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            UsuarioRepository usuarioRepository,
            ConversaRepository conversaRepository,
            MensagemRepository mensagemRepository) {

        this.objectMapper = objectMapper;
        this.usuarioRepository = usuarioRepository;
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws IOException {

        Long usuarioId = (Long) session.getAttributes().get("usuarioId");
        if (usuarioId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        sessoes.add(session);

        System.out.println("Usuário " + usuarioId + " conectado.");
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessoes.remove(session);
        System.out.println("Cliente desconectado: " + session.getId());
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) throws IOException {

        Map<String, Object> entrada;

        try {
            entrada = objectMapper.readValue(
                    message.getPayload(),
                    Map.class
            );
        } catch (Exception e) {
            enviarErro(session, "JSON inválido.");
            return;
        }

        Object tipo = entrada.get("tipo");

        if ("abrir_conversa".equals(tipo)) {
            abrirConversa(session, message.getPayload());
        } else {
            receberMensagem(session, message.getPayload());
        }
    }

    private void abrirConversa(
            WebSocketSession session,
            String payload) throws IOException {

        AbrirConversaRequest request;

        try {
            request = objectMapper.readValue(
                    payload,
                    AbrirConversaRequest.class
            );
        } catch (Exception e) {
            enviarErro(session, "Solicitação de conversa inválida.");
            return;
        }

        Long usuarioId =
                (Long) session.getAttributes().get("usuarioId");

        if (request.conversaId() == null) {
            enviarErro(session, "Informe o ID da conversa.");
            return;
        }

        Conversa conversa = conversaRepository
                .findById(request.conversaId())
                .orElse(null);

        if (conversa == null) {
            enviarErro(session, "Conversa não encontrada.");
            return;
        }

        boolean participante =
                conversa.getUsuario1().getId().equals(usuarioId)
                        || conversa.getUsuario2().getId().equals(usuarioId);

        if (!participante) {
            enviarErro(session, "Você não participa desta conversa.");
            return;
        }

        // Registra qual conversa esta conexão está visualizando.
        session.getAttributes().put("conversaId", conversa.getId());

        // Busca as mensagens antigas em ordem cronológica.
        List<Mensagem> mensagens =
                mensagemRepository
                        .findByConversaIdOrderByDataHoraAsc(conversa.getId());

        List<Map<String, Object>> historico = mensagens.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "conteudo", m.getConteudo(),
                        "remetenteId", m.getRemetente().getId(),
                        "remetenteNome", m.getRemetente().getNome(),
                        "dataHora", m.getDataHora().toString()
                ))
                .toList();

        String resposta = objectMapper.writeValueAsString(
                Map.of(
                        "tipo", "historico",
                        "conversaId", conversa.getId(),
                        "mensagens", historico
                )
        );

        enviar(session, resposta);

        System.out.println(
                "Histórico da conversa " + conversa.getId()
                        + " enviado para usuário " + usuarioId
        );
    }

    private void receberMensagem(
            WebSocketSession session,
            String payload) throws IOException {

        MensagemWebSocketRequest request;

        try {
            request = objectMapper.readValue(
                    payload,
                    MensagemWebSocketRequest.class
            );
        } catch (Exception e) {
            enviarErro(session, "JSON inválido.");
            return;
        }

        if (request.conversaId() == null
                || request.conteudo() == null
                || request.conteudo().isBlank()) {

            enviarErro(
                    session,
                    "Informe conversaId e conteudo."
            );
            return;
        }

        Long usuarioConectado =
                (Long) session.getAttributes().get("usuarioId");

        Conversa conversa = conversaRepository
                .findById(request.conversaId())
                .orElse(null);

        Usuario remetente = usuarioRepository
                .findById(usuarioConectado)
                .orElse(null);

        if (conversa == null || remetente == null) {
            enviarErro(
                    session,
                    "Conversa ou usuário não encontrado."
            );
            return;
        }

        boolean participante =
                conversa.getUsuario1().getId().equals(remetente.getId())
                        || conversa.getUsuario2().getId()
                        .equals(remetente.getId());

        if (!participante) {
            enviarErro(session, "Você não participa desta conversa.");
            return;
        }

        Mensagem novaMensagem = new Mensagem(
                request.conteudo(),
                remetente,
                conversa
        );

        Mensagem mensagemSalva =
                mensagemRepository.save(novaMensagem);

        String resposta = objectMapper.writeValueAsString(
                Map.of(
                        "tipo", "nova_mensagem",
                        "id", mensagemSalva.getId(),
                        "conversaId", conversa.getId(),
                        "conteudo", mensagemSalva.getConteudo(),
                        "remetenteId", remetente.getId(),
                        "remetenteNome", remetente.getNome(),
                        "dataHora", mensagemSalva.getDataHora().toString()
                )
        );

        // Envia somente para as conexões que abriram esta conversa.
        for (WebSocketSession sessao : sessoes) {

            Object conversaAtiva =
                    sessao.getAttributes().get("conversaId");

            if (sessao.isOpen()
                    && conversa.getId().equals(conversaAtiva)) {

                try {
                    enviar(sessao, resposta);
                } catch (IOException e) {
                    System.err.println(
                            "Falha ao enviar para a sessão "
                                    + sessao.getId()
                    );
                }
            }
        }
    }

    private void enviar(
            WebSocketSession session,
            String resposta) throws IOException {

        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(resposta));
            }
        }
    }

    private void enviarErro(
            WebSocketSession session,
            String mensagem) throws IOException {

        String resposta = objectMapper.writeValueAsString(
                Map.of(
                        "tipo", "erro",
                        "erro", mensagem
                )
        );

        enviar(session, resposta);
    }
}
