/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reuven.websocket.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocket.dto.MessageRequest;
import com.reuven.websocket.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MessageHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String DELAY_SERVICE_URI = "http://{%s}:8081/api/delay/{%s}";
    private final String delayServiceHost;

    private final ObjectMapper objectMapper;
    private final WsConnMng wsConnMng;
    private final RestClient restClient;

    public MessageHandler(ObjectMapper objectMapper, WsConnMng wsConnMng, RestClient restClient,
                          @Value("${delay.service.host}") String delayServiceHost) {
        this.objectMapper = objectMapper;
        this.wsConnMng = wsConnMng;
        this.restClient = restClient;
        this.delayServiceHost = delayServiceHost;
        logger.info("running on DELAY_SERVICE_HOST {}", delayServiceHost);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        Object payload = message.getPayload();
        logger.info("SessionId={}. Received message: {}", session.getId(), payload);
        MessageRequest data = readValue(payload.toString());
        logger.info("row data: {}", data);

        // Call the delay service (blocking)
        String responseData = restClient.get()
                .uri(DELAY_SERVICE_URI, delayServiceHost, session.getId())
                .retrieve()
                .body(String.class);

        logger.info("Response from delay service: {}", responseData);

        // Send the response back to the client (blocking)
        MessageResponse messageResponse = new MessageResponse(UUID.randomUUID(), "Response from delay service: " + responseData, LocalDateTime.now());
        session.sendMessage(new TextMessage(writeValueAsString(messageResponse)));

//        session.close(CloseStatus.NORMAL);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        wsConnMng.addSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("Error occurred: {}", exception.getMessage());
        wsConnMng.removeSession(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        wsConnMng.removeSession(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private <T> String writeValueAsString(T msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageRequest readValue(String text) {
        try {
            return objectMapper.readValue(text, MessageRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
