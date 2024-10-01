package com.reuven.websocketreactive.services;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsConnMngImpl implements WsConnMng {

    private static final Logger logger = LoggerFactory.getLogger(WsConnMngImpl.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.info("Session established: {}", session.getId());
    }

    @Override
    public void removeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            session.close();
        }
        logger.info("Session closed: {}", sessionId);
    }

    @Override
    public WebSocketSession getSession(String sessionId) {
        logger.info("Session retrieved: {}", sessionId);
        return sessions.get(sessionId);
    }

    @PreDestroy
    public void close() {
        sessions.forEach((sessionId, session) -> {
            if (session != null && session.isOpen()) {
                session.close();
            }
        });
    }
}

