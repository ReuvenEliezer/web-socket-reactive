package com.reuven.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reuven.websocket.dto.MessageRequest;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = WebSocketApplication.class)
@ActiveProfiles("test")
class WebSocketApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketApplicationTests.class);

    private final WebSocketClient client = new StandardWebSocketClient();
    private static final String WEB_SOCKET_URI_STR = "ws://localhost:%s/ws/messages";
    private static URI WEB_SOCKET_URI;

    @Value("${server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Value("${ws.open-connection-duration}")
    private Duration wsOpenConnectionDuration;

    @PostConstruct
    void init() {
        WEB_SOCKET_URI = URI.create(String.format(WEB_SOCKET_URI_STR, port));
    }

    @Test
    void webSocketManyMessagesForManySessionsTest() throws Exception {
        int totalConnections = 2;
        int numOfMessagesForEachConnection = 3;
        CountDownLatch latch = new CountDownLatch(totalConnections * numOfMessagesForEachConnection);

        List<CompletableFuture<Void>> futures = sendMessages(totalConnections, numOfMessagesForEachConnection, latch);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        if (!latch.await(wsOpenConnectionDuration.plusSeconds(2).getSeconds(), TimeUnit.SECONDS)) {
            logger.error("Not all connections completed in time");
        }
//        Thread.sleep(wsOpenConnectionDuration.plusSeconds(2).toMillis());
        assertThat(latch.getCount()).isZero();
    }

    private List<CompletableFuture<Void>> sendMessages(int totalConnections, int numOfMessagesForEachConnection, CountDownLatch latch) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(totalConnections);
        for (int i = 0; i < totalConnections; i++) {
            CompletableFuture<WebSocketSession> sessionFuture = client.execute(webSocketHandler, WEB_SOCKET_URI.toString());
            int connNum = i;
            CompletableFuture<Void> testMessage = sessionFuture.thenAccept(session -> {
                try {
                    for (int j = 0; j < numOfMessagesForEachConnection; j++) {
                        String messagePayload = writeValueAsString(new MessageRequest(String.format("Test Message number '%s' for session number '%s' sessionId: %s", j + 1, connNum + 1, session.getId())));
                        logger.info("Sending message: {}", messagePayload);
                        session.sendMessage(new TextMessage(messagePayload));
                        latch.countDown();
                    }
                } catch (Exception e) {
                    logger.error("Error sending message", e);
                    throw new RuntimeException(e);
                } finally {
//                    if (session.isOpen()) {
//                        try {
//                            session.close();
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
                }
            });
            futures.add(testMessage);
        }
        return futures;
    }


    private String writeValueAsString(MessageRequest msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
