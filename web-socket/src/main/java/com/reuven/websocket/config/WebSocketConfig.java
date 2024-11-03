package com.reuven.websocket.config;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    public WebSocketConfig(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/messages")
                .setAllowedOrigins("*")
//                .addInterceptors(new HttpSessionHandshakeInterceptor())
//                .withSockJS()
//                .setStreamBytesLimit(512 * 1024)
//                .setHttpMessageCacheSize(1000)
//                .setDisconnectDelay(Duration.ofMinutes(1).toMillis())
        ;
    }


//    @Bean
//    public HttpClient httpClient() throws Exception {
//        HttpClient jettyHttpClient = new HttpClient();
//        jettyHttpClient.setMaxConnectionsPerDestination(1000);
//        jettyHttpClient.setExecutor(new QueuedThreadPool(1000));
//        jettyHttpClient.setConnectTimeout(Duration.ofSeconds(10).toMillis());
//        jettyHttpClient.setIdleTimeout(Duration.ofSeconds(10).toMillis());
//        jettyHttpClient.setDestinationIdleTimeout(Duration.ofSeconds(10).toMillis());
//        jettyHttpClient.start();
//        return jettyHttpClient;
//    }


    @Bean
    @ConditionalOnExpression("!environment.acceptsProfiles('test')")
//    @Profile("!test")
    public ServletServerContainerFactoryBean servletServerContainerFactoryBean(
            @Value("${ws.open-connection-duration}") Duration wsOpenConnectionDuration
    ) {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(wsOpenConnectionDuration.toMillis());
        return container;
    }

//    @Bean
//    public ThreadPoolExecutor tomcatThreadPool() {
//        int corePoolSize = 300;
//        int maxPoolSize = 1000;
//        long keepAliveTime = 60L;
//        return new ThreadPoolExecutor(
//                corePoolSize,
//                maxPoolSize,
//                keepAliveTime,
//                TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>()
//        );
//    }

}

