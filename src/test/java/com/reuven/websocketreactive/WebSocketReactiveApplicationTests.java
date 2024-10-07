package com.reuven.websocketreactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocketreactive.dto.MessageRequest;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;

//@SpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = WebSocketReactiveApplication.class)
@ActiveProfiles("test")
class WebSocketReactiveApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketReactiveApplicationTests.class);

    private final WebSocketClient client = new ReactorNettyWebSocketClient();
    private static final String WEB_SOCKET_URI_STR = "ws://localhost:%s/ws/messages";
    private static URI WEB_SOCKET_URI;

    @Value("${server.port}")
    private int port;

    @Value("${ws.open-connection-duration}")
    private Duration wsOpenConnectionDuration;


    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        WEB_SOCKET_URI = URI.create(String.format(WEB_SOCKET_URI_STR, port));
    }


    @Test
    void webSocketReactivePerformanceTest() {
        int totalConnections = 50;
        int totalMessagesForEachConnection = 1000;

//        PerformanceMonitor performanceMonitor = new PerformanceMonitor();
//        performanceMonitor.startMonitoring(20);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Flux<Void> createConnections = Flux.range(0, totalConnections)
//                .delayElements(Duration.ofMillis(100))
                .flatMap(connNum -> client.execute(WEB_SOCKET_URI, session -> {
                            Flux<WebSocketMessage> messages = Flux.range(0, totalMessagesForEachConnection)
                                    .map(msgNum -> toString(new MessageRequest(String.format("Test Message number '%s' for connection-number '%s' sessionId: %s", msgNum + 1, connNum + 1, session.getId()))))
                                    .doOnNext(msgStr -> logger.info("Sending message: {}", msgStr))
                                    .map(session::textMessage);
                            return session.send(messages)
                                    .thenMany(session.receive()
                                            .map(WebSocketMessage::getPayloadAsText)
                                            .log())
                                    .then();
                        })
                );

//        Flux<Void> createConnections = Flux.range(0, totalConnections)
//                .flatMap(connNum ->
//                        client.execute(WEB_SOCKET_URI, session -> {
//                            Flux<String> messages = Flux.range(0, totalMessagesForEachConnection)
//                                    .map(msgNum -> toString(new MessageRequest(String.format("Test Message number '%s' for connection-number '%s' sessionId: %s", msgNum + 1, connNum + 1, session.getId()))))
//                                    .doOnNext(msgStr -> logger.info("Sending message: {}", msgStr));
//                            return session.send(messages.map(session::textMessage));
//                        })
//                );
//        createConnections.blockLast();

        waitCompletion(createConnections, wsOpenConnectionDuration.plusMinutes(1));
        stopWatch.stop();

        logger.info("Reactive WebSocket took: {}, shortSummary: {}", stopWatch.prettyPrint(), stopWatch.shortSummary());
    }


    private static void waitCompletion(Flux<Void> createConnections, Duration maxWaitingTime) {
        StepVerifier.create(createConnections)
                .expectComplete()
                .verify(maxWaitingTime);
    }

    private URI buildUri() {
        return URI.create("ws://localhost:" + port + "/ws/messages");
    }


    private String toString(MessageRequest msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
