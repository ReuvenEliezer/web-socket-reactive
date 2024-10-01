package com.reuven.websocketreactive.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorNetty2ResourceFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorNetty2HttpHandlerAdapter;
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
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.netty5.http.server.HttpServer;
import reactor.netty5.resources.ConnectionProvider;
import reactor.netty5.resources.LoopResources;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableWebFlux
public class WebConfig {

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
    //TODO check way the socket not disconnected after 10 sec of idle
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("custom-connection-pool")
                .maxConnections(1)
                .maxIdleTime(Duration.ofSeconds(10))
                .pendingAcquireTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Bean
    public ReactorNetty2ResourceFactory resourceFactory(ConnectionProvider connectionProvider, LoopResources loopResources) {
        ReactorNetty2ResourceFactory factory = new ReactorNetty2ResourceFactory();
        factory.setConnectionProvider(connectionProvider);
        factory.setLoopResources(loopResources);
        factory.setUseGlobalResources(false);
        return factory;
    }

    @Bean
    public HttpServer nettyHttpServer(ApplicationContext context) {
        HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context).build();
        ReactorNetty2HttpHandlerAdapter adapter = new ReactorNetty2HttpHandlerAdapter(handler);
        HttpServer httpServer = HttpServer.create().host("localhost").port(8080);
        return httpServer.handle(adapter);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

}
