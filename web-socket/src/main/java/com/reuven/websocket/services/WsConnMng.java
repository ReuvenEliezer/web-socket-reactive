package com.reuven.websocket.services;

import org.springframework.web.socket.WebSocketSession;

public interface WsConnMng {

    void addSession(WebSocketSession session);

    void removeSession(String sessionId);

    WebSocketSession getSession(String sessionId);
}
