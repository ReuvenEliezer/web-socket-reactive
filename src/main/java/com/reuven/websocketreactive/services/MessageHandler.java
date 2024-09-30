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

    private final Sinks.Many<Message> sinks = Sinks.many().replay().limit(2);
    private final Flux<Message> outputMessages = sinks.asFlux();

    private final ObjectMapper objectMapper;
    private final WSConnectionMng wsConnectionMng;

    public MessageHandler(ObjectMapper objectMapper, WSConnectionMng wsConnectionMng) {
        this.objectMapper = objectMapper;
        this.wsConnectionMng = wsConnectionMng;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        wsConnectionMng.addSession(session);
        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::readIncomingMessage)
                .flatMap(req -> {
                    return Mono.just(new Message(UUID.randomUUID(), req.message(), LocalDateTime.now()));
//                    return toJsonString(responseMessage);
//                    return session.send(Mono.just(session.textMessage(jsonResponse)));
                })
                .doOnNext(data -> logger.info("data: {}", data))
                .doFinally(signalType -> wsConnectionMng.removeSession(session.getId()))
                .then();
    }



//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//
//        logger.info("Session established:: {}", session.getId());
//
//        var receiveMono = session.receive()
//                .map(WebSocketMessage::getPayloadAsText)
//                .map(this::readIncomingMessage)
//                .flatMap(req ->
//                        Mono.fromCallable(() -> new Message(UUID.randomUUID(), req.message(), LocalDateTime.now()))
//                                .subscribeOn(Schedulers.boundedElastic())
//                )
//                .doOnNext(data -> {
//                    logger.info("data: {}", data);
//                    sinks.emitNext(data, Sinks.EmitFailureHandler.FAIL_FAST);
//                })
//                .doOnError(error -> sinks.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST))
//                .then();
//
//
////        var receiveMono = session.receive()
////                .map(WebSocketMessage::getPayloadAsText)
////                .map(this::readIncomingMessage)
////                .flatMap(req ->
////                        Mono.fromCallable(
////                                () -> new Message(UUID.randomUUID(), req.message(), LocalDateTime.now())
////                        )
////                )
////                .log("server receiving::")
//////                .subscribe(
//////                        data -> sinks.emitNext(data, Sinks.EmitFailureHandler.FAIL_FAST),
//////                        error -> sinks.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST)
//////                );
////                .doOnNext(data -> {
////                    logger.info("data: {}", data);
////                    sinks.emitNext(data, Sinks.EmitFailureHandler.FAIL_FAST);
////                })
////                .doOnError(error -> sinks.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST))
////                .then();
//
//        // TODO: workaround for suspected RxNetty WebSocket client issue
//        // https://github.com/ReactiveX/RxNetty/issues/560
//        var sendMono = session
//                .send(
//                        Mono.delay(Duration.ofMillis(500))
//                                .thenMany(outputMessages.map(msg -> session.textMessage(toJsonString(msg))))
//                )
//                .log("server sending::")
//                .onErrorResume(throwable -> session.close())
//                .then();
//
//        return Mono.zip(receiveMono, sendMono).then();
//        //return sendMono;
//    }

    private String toJsonString(Message msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageRequest readIncomingMessage(String text) {
        try {
            return objectMapper.readValue(text, MessageRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
