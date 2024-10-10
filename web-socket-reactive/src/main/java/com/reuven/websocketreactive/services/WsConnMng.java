package com.reuven.websocketreactive.services;

import org.springframework.web.reactive.socket.WebSocketSession;

public interface WsConnMng {

    void addSession(WebSocketSession session);

    void removeSession(String sessionId);

    WebSocketSession getSession(String sessionId);
}
