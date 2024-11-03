package com.reuven.websocketreactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocketreactive.dto.MessageRequest;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = WebSocketReactiveApplication.class)
//@ActiveProfiles("test")
class WebSocketReactiveApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketReactiveApplicationTests.class);

    private final WebSocketClient client = new ReactorNettyWebSocketClient();

    private static final String WEB_SOCKET_URI_STR = "ws://localhost:%s/ws/messages";
    private static URI WEB_SOCKET_URI;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${server.port}")
    private int port;

    @Value("${ws.open-connection-duration}")
    private Duration wsOpenConnectionDuration;


    @PostConstruct
    void init() {
        WEB_SOCKET_URI = URI.create(String.format(WEB_SOCKET_URI_STR, port));
    }


    @Test
    void webSocketReactivePerformanceTest() {
        int totalConnections = 1000; //2;
        int totalMessagesForEachConnection = 50; //3;

        Duration delayBetweenMessagesElementsDuration = Duration.ofMillis(200);
        Duration delayBetweenOpenConnectionsDuration = Duration.ofMillis(100);
        Duration totalWaitingTimeDuration = delayBetweenMessagesElementsDuration.multipliedBy(totalMessagesForEachConnection)
                .plus(delayBetweenOpenConnectionsDuration.multipliedBy(totalConnections));
        List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>(totalConnections * totalMessagesForEachConnection));
//        CountDownLatch latch = new CountDownLatch(totalConnections * totalMessagesForEachConnection);

//        ResourceMonitor monitor = new ResourceMonitor();
//        monitor.startMonitoring();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Flux<Void> createConnections = Flux.range(0, totalConnections)
                .delayElements(delayBetweenOpenConnectionsDuration)
                .flatMap(connNum -> client.execute(WEB_SOCKET_URI, session -> {
                            Flux<WebSocketMessage> messages = Flux.range(0, totalMessagesForEachConnection)
                                    .delayElements(delayBetweenMessagesElementsDuration)
                                    .map(msgNum -> toString(new MessageRequest(String.format("Test Message number '%s' for connection-number '%s' sessionId: %s", msgNum + 1, connNum + 1, session.getId()))))
                                    .doOnNext(msgStr -> {
                                        logger.info("Sending message: {}", msgStr);
//                                        latch.countDown();
                                    })
                                    .map(session::textMessage);
                            return session.send(messages)
                                    .thenMany(session.receive()
                                            .map(WebSocketMessage::getPayloadAsText)
                                            .doOnNext(msg -> {
                                                logger.info("Received message: {}", msg);
                                                receivedMessages.add(msg);
                                            })
                                    )
                                    .then();
                        })
                );


        waitCompletion(createConnections, totalWaitingTimeDuration.plusMinutes(3));
        stopWatch.stop();
//        monitor.stopMonitoring();
        assertFalse(receivedMessages.isEmpty(), "No messages were received!");
        assertThat(receivedMessages).hasSize(totalConnections * totalMessagesForEachConnection);
        assertTrue(receivedMessages.stream().allMatch(msg -> msg.contains("Response from delay service")),
                "Received messages do not contain expected response from delay service.");
        logger.info("Reactive WebSocket took: {}, shortSummary: {}", stopWatch.prettyPrint(), stopWatch.shortSummary());
    }

    private static void waitCompletion(Flux<Void> createConnections, Duration maxWaitingTime) {
        StepVerifier.create(createConnections)
                .expectComplete()
                .verify(maxWaitingTime);
    }

    private String toString(MessageRequest msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
