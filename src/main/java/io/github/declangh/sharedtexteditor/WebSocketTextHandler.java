package io.github.declangh.sharedtexteditor;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebSocketTextHandler extends TextWebSocketHandler{

    // kj
    private WebSocketSession leaderSession = null;
    private final CopyOnWriteArrayList<WebSocketSession> allowedSessions
            = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (leaderSession == null) {
            leaderSession = session;
            session.sendMessage(new TextMessage("You are the leader"));
        } else {
            session.sendMessage(new TextMessage("Waiting for leader's approval"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        // Broadcast
        for (WebSocketSession webSocketSession : allowedSessions) {
            if (webSocketSession.isOpen() && !session.getId().equals(webSocketSession.getId())) {
                webSocketSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (session.equals(leaderSession)) {
            // End the session for everyone
            for (WebSocketSession s : allowedSessions) {
                if (s.isOpen()) {
                    s.close();
                }
            }
            leaderSession = null;
            allowedSessions.clear();
        } else {
            allowedSessions.remove(session);
        }
    }
}
