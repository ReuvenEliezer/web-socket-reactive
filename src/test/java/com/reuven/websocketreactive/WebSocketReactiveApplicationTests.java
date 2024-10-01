package com.reuven.websocketreactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocketreactive.dto.MessageRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;

@SpringBootTest
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = WebSocketReactiveApplication.class)
class WebSocketReactiveApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketReactiveApplicationTests.class);

    private final WebSocketClient client = new ReactorNettyWebSocketClient();

    @Value("${server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testReactiveWebSocketPerformance1() {
        Flux<Void> createConnections = Flux.range(0, 3)
                .flatMap(i ->
                        client.execute(buildUrl(), session -> {
                            Flux<String> messages = Flux.range(0, 2)
                                    .map(j -> toString(new MessageRequest(String.format("Test Message %s for connection: %s", i, j))));
                            return session.send(messages.map(session::textMessage)).then();
                        })
                );

        StepVerifier.create(createConnections)
                .expectComplete()
                .verify(Duration.ofSeconds(60));
    }


    @Test
    public void testReactiveWebSocketPerformance2() {
        Flux<Void> createConnections = Flux.range(0, 2)
                .delayElements(Duration.ofMillis(100))
                .flatMap(i ->
                        client.execute(buildUrl(), session -> {
                            String msgStr = toString(new MessageRequest("Test Message " + i));
                            return session.send(Mono.just(session.textMessage(msgStr)));
                        })
                );

        StepVerifier.create(createConnections)
                .expectComplete()
                .verify(Duration.ofSeconds(60));
    }

    private URI buildUrl() {
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
