package com.reuven.websocketreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import reactor.tools.agent.ReactorDebugAgent;

@ComponentScan(basePackages = {
        "com.reuven.websocketreactive.services",
        "com.reuven.websocketreactive.config"
})
@SpringBootApplication
public class WebSocketReactiveApplication {

    public static void main(String[] args) {
        ReactorDebugAgent.init();
        SpringApplication.run(WebSocketReactiveApplication.class, args);
    }

}
