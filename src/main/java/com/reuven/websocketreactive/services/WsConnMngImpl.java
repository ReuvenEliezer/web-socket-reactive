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
        closeSession(session);
    }

    @Override
    public WebSocketSession getSession(String sessionId) {
        logger.info("Session retrieved: {}", sessionId);
        return sessions.get(sessionId);
    }

    private static void closeSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                session.close()
                        .doOnSuccess(aVoid -> logger.info("Session closed: {}", session.getId()))
                        .doOnError(e -> logger.error("Error occurred while closing session: {}", e.getMessage()))
                        .subscribe();
                logger.info("Session closed: {}", session.getId());
            } catch (Exception e) {
                logger.error("Error occurred: {}", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void close() {
        sessions.forEach((sessionId, session) -> {
            closeSession(session);
        });
    }
}

