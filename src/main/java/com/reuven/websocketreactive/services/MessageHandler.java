/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reuven.websocketreactive.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocketreactive.dto.Message;
import com.reuven.websocketreactive.dto.MessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MessageHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String DELAY_SERVICE_URI = "http://localhost:8081/api/delay/{%s}";

    private final ObjectMapper objectMapper;
    private final WsConnMng wsConnMng;
    private final WebClient webClient;

    public MessageHandler(ObjectMapper objectMapper, WsConnMng wsConnMng, WebClient webClient) {
        this.objectMapper = objectMapper;
        this.wsConnMng = wsConnMng;
        this.webClient = webClient;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        wsConnMng.addSession(session);

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::readValue)
                .doOnNext(data -> logger.info("row data: {}", data))
                .onErrorResume(e -> {
                    logger.error("Error occurred: {}", e.getMessage());
                    return Mono.empty();
                })
//                .flatMap(req -> {
//                    logger.info("req: {}", req);
//                    return session.send(session.textMessage(toString(new Message(UUID.randomUUID(), req.message(), LocalDateTime.now()))));
//                })
//                .flatMap(req -> {
//                    logger.info("req: {}", req);
//                    return Mono.delay(Duration.ofMillis(300))
//                            .flatMap(delay -> session.send(Mono.just(session.textMessage(
//                                    toString(new Message(UUID.randomUUID(), req.message(), LocalDateTime.now()))
//                            ))));
//                })
                .flatMap(requestMessage -> webClient.get()
                        .uri(DELAY_SERVICE_URI, session.getId())
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnNext(data -> logger.info("Response from delay service: {}", data))
                        .flatMap(responseData -> session.send(Mono.just(session.textMessage("Response from delay service: " + responseData))))
                        .onErrorResume(sendError -> {
                            logger.error("Error during message send: {}", sendError.getMessage());
                            return Mono.empty();
                        }))
                .doOnTerminate(() -> logger.info("Session {} terminated", session.getId()))
                .doOnCancel(() -> logger.info("Session {} cancelled", session.getId()))
                .doOnError(e -> logger.error("Session {} error: {}", session.getId(), e.getMessage()))
                .doFinally(signalType -> {
                    wsConnMng.removeSession(session.getId());
                })
                .then()
                ;
    }

    private <T> String toString(T msg) {
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
