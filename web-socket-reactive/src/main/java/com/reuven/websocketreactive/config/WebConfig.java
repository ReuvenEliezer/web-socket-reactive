package com.reuven.websocketreactive.config;

import brave.Tracing;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

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

//    @Bean
//    public LoopResources loopResources() {
//        // Create custom LoopResources for Reactor Netty 5
//        return LoopResources.create("netty5-event-loop", 1, 1, true);
//    }
//
//    @Bean
//    //TODO check way the socket not disconnected after 10 sec of idle
//    public ConnectionProvider connectionProvider() {
//        return ConnectionProvider.builder("custom-connection-pool")
//                .maxConnections(1000)
//                .maxIdleTime(Duration.ofMinutes(10))
//                .pendingAcquireTimeout(Duration.ofMinutes(2))
//                .build();
//    }
//
//    @Bean
//    public ReactorNetty2ResourceFactory resourceFactory(ConnectionProvider connectionProvider, LoopResources loopResources) {
//        ReactorNetty2ResourceFactory factory = new ReactorNetty2ResourceFactory();
//        factory.setConnectionProvider(connectionProvider);
//        factory.setLoopResources(loopResources);
//        factory.setUseGlobalResources(false);
//        return factory;
//    }
//
//    @Bean
//    public HttpServer nettyHttpServer(ApplicationContext context, @Value("${server.port}") int port) {
//        HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context).build();
//        ReactorNetty2HttpHandlerAdapter adapter = new ReactorNetty2HttpHandlerAdapter(handler);
//        HttpServer httpServer = HttpServer.create().host("localhost").port(port);
//        return httpServer.handle(adapter);
//    }

    @Bean
    public WebClient webClient() {
//        TcpClient tcpClient = TcpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
//                .doOnConnected(connection ->
//                        connection.addHandlerLast(new LoggingHandler(LogLevel.TRACE))
//                                .addHandlerLast(new ReadTimeoutHandler(30))
//                                .addHandlerLast(new WriteTimeoutHandler(30)));
//        tcpClient.wiretap(true);
//
//        ReactorClientHttpConnector httpConnector = new ReactorClientHttpConnector(HttpClient.from(tcpClient));
//
//
//        return WebClient.builder()
//                .clientConnector(httpConnector)
//                .build();
//
//        HttpClient httpClient = HttpClient.create()
//                .responseTimeout(Duration.ofSeconds(30));
//
//        return WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .build();

//        ConnectionProvider connectionProvider = ConnectionProvider.builder("myConnectionPool")
//                .maxConnections(500)
//                .pendingAcquireMaxCount(5000)
//                .pendingAcquireTimeout(Duration.ofSeconds(3))  // Pool#acquire(Duration) has been pending for more than the configured timeout of 45000ms
//                .build();
//        ReactorClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(HttpClient.create(connectionProvider));
//        return WebClient.builder()
//                .clientConnector(clientHttpConnector)
//                .build();
        return WebClient.create();
    }

}
