package com.reuven.websocketreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {
        "com.reuven.websocketreactive.services",
        "com.reuven.websocketreactive.config"
})
@SpringBootApplication
public class WebSocketReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketReactiveApplication.class, args);
    }

}
