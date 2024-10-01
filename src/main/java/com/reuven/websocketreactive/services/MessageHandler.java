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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MessageHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String DELAY_SERVICE_URI = "http://localhost:8081/api/delay/{%s}";

    private final ObjectMapper objectMapper;
    private final WSConnectionMng wsConnectionMng;
    private final WebClient webClient;

    public MessageHandler(ObjectMapper objectMapper, WSConnectionMng wsConnectionMng, WebClient webClient) {
        this.objectMapper = objectMapper;
        this.wsConnectionMng = wsConnectionMng;
        this.webClient = webClient;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        wsConnectionMng.addSession(session);

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::readValue)
                .doOnNext(data -> logger.info("row data: {}", data))
//                .flatMap(req -> {
//                    logger.info("req: {}", req);
//                    return session.send(Mono.just(session.textMessage(toString(new Message(UUID.randomUUID(), req.message(), LocalDateTime.now())))));
//                })
                .flatMap(requestMessage ->
                        webClient.get()
                                .uri(DELAY_SERVICE_URI, session.getId())
                                .retrieve()
                                .bodyToMono(String.class)
                                .flatMap(responseData -> session.send(Mono.just(
                                        session.textMessage("Response from delay service: " + responseData)
                                ))))
                .doFinally(signalType -> wsConnectionMng.removeSession(session.getId()))
                .then();
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
