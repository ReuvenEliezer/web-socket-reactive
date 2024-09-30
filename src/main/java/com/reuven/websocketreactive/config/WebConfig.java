package com.reuven.websocketreactive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorNetty2ResourceFactory;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableWebFlux
class WebConfig {

    @Bean
    public HandlerMapping handlerMapping(WebSocketHandler webSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/messages", webSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public WebSocketService webSocketService() {
        RequestUpgradeStrategy upgradeStrategy = new ReactorNettyRequestUpgradeStrategy();
        return new HandshakeWebSocketService(upgradeStrategy);
    }

    @Bean
    public LoopResources loopResources() {
        // Create custom LoopResources for Reactor Netty 5
        return LoopResources.create("netty5-event-loop", 1, 1, true);
    }

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("custom-connection-pool")
                .maxConnections(1)
                .maxIdleTime(Duration.ofSeconds(10))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public ReactorNetty2ResourceFactory resourceFactory(ConnectionProvider connectionProvider, LoopResources loopResources) {
        ReactorNetty2ResourceFactory factory = new ReactorNetty2ResourceFactory();
        factory.setConnectionProvider(connectionProvider);
        factory.setLoopResources(loopResources);
        return factory;
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

}
