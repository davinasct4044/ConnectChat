
package com.connectchat.handler;

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

        String query = session.getUri() != null
                ? session.getUri().getQuery()
                : null;

        Long usuarioId = null;

        if (query != null) {
            for (String parametro : query.split("&")) {
                String[] partes = parametro.split("=", 2);

                if (partes.length == 2
                        && partes[0].equals("usuarioId")) {
                    try {
                        usuarioId = Long.parseLong(partes[1]);
                    } catch (NumberFormatException ignored) {
                        // O ID informado não é numérico.
                    }
                }
            }
        }

        if (usuarioId == null
                || !usuarioRepository.existsById(usuarioId)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        session.getAttributes().put("usuarioId", usuarioId);
        sessoes.add(session);

        System.out.println(
                "Usuário " + usuarioId
                        + " conectado: " + session.getId()
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessoes.remove(session);

        System.out.println(
                "Cliente desconectado: " + session.getId()
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) throws IOException {

        MensagemWebSocketRequest request;

        try {
            request = objectMapper.readValue(
                    message.getPayload(),
                    MensagemWebSocketRequest.class
            );
        } catch (Exception e) {
            enviarErro(session, "JSON inválido.");
            return;
        }

        if (request.conversaId() == null
                || request.remetenteId() == null
                || request.conteudo() == null
                || request.conteudo().isBlank()) {

            enviarErro(
                    session,
                    "Informe conversaId, remetenteId e conteudo."
            );
            return;
        }

        // Busca a conversa e o remetente.
        Conversa conversa = conversaRepository
                .findById(request.conversaId())
                .orElse(null);

        Usuario remetente = usuarioRepository
                .findById(request.remetenteId())
                .orElse(null);

        if (conversa == null || remetente == null) {
            enviarErro(
                    session,
                    "Conversa ou usuário não encontrado."
            );
            return;
        }

        // Verifica se o remetente participa da conversa.
        boolean participante =
                conversa.getUsuario1().getId().equals(remetente.getId())
                        || conversa.getUsuario2().getId()
                        .equals(remetente.getId());

        if (!participante) {
            enviarErro(
                    session,
                    "Você não participa desta conversa."
            );
            return;
        }

        // Cria e salva a mensagem no banco.
        Mensagem novaMensagem = new Mensagem(
                request.conteudo(),
                remetente,
                conversa
        );

        Mensagem mensagemSalva =
                mensagemRepository.save(novaMensagem);

        // Monta o JSON que será enviado aos participantes.
        String resposta = objectMapper.writeValueAsString(
                Map.of(
                        "id", mensagemSalva.getId(),
                        "conversaId", conversa.getId(),
                        "conteudo", mensagemSalva.getConteudo(),
                        "remetenteId", remetente.getId(),
                        "remetenteNome", remetente.getNome(),
                        "dataHora",
                        mensagemSalva.getDataHora().toString()
                )
        );

        // Distribui a mensagem para as sessões dos dois participantes.
        for (WebSocketSession sessao : sessoes) {

            if (!sessao.isOpen()) {
                continue;
            }

            Object usuarioSessao =
                    sessao.getAttributes().get("usuarioId");

            if (!(usuarioSessao instanceof Long id)) {
                continue;
            }

            boolean fazParteDaConversa =
                    id.equals(conversa.getUsuario1().getId())
                            || id.equals(conversa.getUsuario2().getId());

            if (!fazParteDaConversa) {
                continue;
            }

            try {
                synchronized (sessao) {
                    if (sessao.isOpen()) {
                        sessao.sendMessage(
                                new TextMessage(resposta)
                        );
                    }
                }

                System.out.println(
                        "Mensagem " + mensagemSalva.getId()
                                + " enviada para usuário " + id
                );

            } catch (IOException e) {
                System.err.println(
                        "Erro ao enviar mensagem para usuário "
                                + id + ": " + e.getMessage()
                );
            }
        }
    }

    private void enviarErro(
            WebSocketSession session,
            String mensagem) throws IOException {

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(
                    objectMapper.writeValueAsString(
                            Map.of("erro", mensagem)
                    )
            ));
        }
    }
}
