package com.reuven.websocketreactive.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocketreactive.dto.MessageRequest;
import com.reuven.websocketreactive.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MessageHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String DELAY_SERVICE_URI = "http://{%s}:8081/api/delay/{%s}";

    //    public static final Duration WS_OPEN_CONNECTION_DURATION = Duration.ofMinutes(10);  // timeout for disconnection socket without actions
    private static final Duration FIXED_DELAY_ON_RETRY = Duration.ofSeconds(1);
    private static final long MAX_RETRY = 3;

    private final ObjectMapper objectMapper;
    private final WsConnMng wsConnMng;
    private final WebClient webClient;
    private final Duration wsOpenConnectionDuration;
    private final String delayServiceHost;

    public MessageHandler(ObjectMapper objectMapper, WsConnMng wsConnMng, WebClient webClient,
                          @Value("${ws.open-connection-duration}") Duration wsOpenConnectionDuration, //timeout for disconnection socket without actions
                          @Value("${delay.service.host}") String delayServiceHost) {
        this.objectMapper = objectMapper;
        this.wsConnMng = wsConnMng;
        this.webClient = webClient;
        this.wsOpenConnectionDuration = wsOpenConnectionDuration;
        this.delayServiceHost = delayServiceHost;
        logger.info("running on DELAY_SERVICE_HOST {}", delayServiceHost);
    }


    @Override
    public Mono<Void> handle(WebSocketSession session) {
        wsConnMng.addSession(session);

        return session.receive()
                .timeout(wsOpenConnectionDuration)
//                .delayElements(Duration.ofMillis(100))
//                .retryWhen(Retry.fixedDelay(MAX_RETRY, FIXED_DELAY_ON_RETRY))
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::readValue)
                .doOnNext(data -> logger.info("row data: {}", data))
                .onErrorResume(e -> {
                    logger.error("Error occurred: {}", e.getMessage());
                    return Mono.empty();
                })
//                .flatMap(req -> {
//                    logger.info("req: {}", req);
//                    return session.send(session.textMessage(writeValueAsString(new MessageResponse(UUID.randomUUID(), req.message(), LocalDateTime.now()))));
//                })
//                .flatMap(req -> {
//                    logger.info("req: {}", req);
//                    return Mono.delay(Duration.ofMillis(1))
//                            .flatMap(delay ->
//                                    session.send(Mono.just(session.textMessage(
//                                            writeValueAsString(new MessageResponse(UUID.randomUUID(), "Response from delay service: " + req.message(), LocalDateTime.now()))
//                                    ))));
//                })
                .flatMap(requestMessage -> webClient.get()
                        .uri(DELAY_SERVICE_URI, delayServiceHost, session.getId())
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(sendError -> {
                            logger.error("Error during call Delay-service: {}", sendError.getMessage());
                            return Mono.empty();
                        })
                        .retryWhen(Retry.fixedDelay(MAX_RETRY, FIXED_DELAY_ON_RETRY))
                        .doOnNext(data -> logger.info("Response from delay service: {}", data))
                        .flatMap(responseData -> session.send(Mono.just(session.textMessage(writeValueAsString(new MessageResponse(UUID.randomUUID(), "Response from delay service: " + responseData, LocalDateTime.now()))))))
                        .onErrorResume(sendError -> {
                            logger.error("Error during message send: {}", sendError.getMessage());
                            return Mono.empty();
                        }))
                .doOnTerminate(() -> logger.info("Session {} terminated", session.getId()))
                .doOnCancel(() -> logger.info("Session {} cancelled", session.getId()))
                .doOnError(e -> logger.error("Session {} error: {}", session.getId(), e.getMessage()))
                .doFinally(signalType -> wsConnMng.removeSession(session.getId()))
                .then()
                ;
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
