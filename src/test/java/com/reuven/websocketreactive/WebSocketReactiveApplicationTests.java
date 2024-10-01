package com.reuven.websocketreactive;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.socket.client.ReactorNetty2WebSocketClient;
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

    private final WebSocketClient client = new ReactorNetty2WebSocketClient();

    @Value("${server.port}")
    private int port;

    @Test
    public void testReactiveWebSocketPerformance() {
        PerformanceMonitor performanceMonitor = new PerformanceMonitor();
        performanceMonitor.startMonitoring(20);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Mono<Void> sendMessages = Flux.range(0, 100)
                .flatMap(i -> client.execute(URI.create("ws://localhost:" + port + "/ws/messages"), session ->
                        session.send(Mono.just(session.textMessage("Message " + i)))
                                .then())
                ).then();

        StepVerifier.create(sendMessages)
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        stopWatch.stop();

        logger.info("Reactive WebSocket took: {}, shortSummary: {}", stopWatch.prettyPrint(), stopWatch.shortSummary());
    }

    @Test
    public void testReactiveWebSocketPerformance1() {
        Mono<Void> createConnections = Flux.range(0, 3)
                .flatMap(i ->
                        client.execute(URI.create("ws://localhost:" + port + "/ws/messages"), session -> {
                            Flux<String> messages = Flux.range(0, 2).map(j -> "Message " + j);
                            return session.send(messages.map(session::textMessage)).then();
                        })
                )
                .then();

        StepVerifier.create(createConnections)
                .expectComplete()
                .verify(Duration.ofSeconds(60));

    }


}
