package com.reuven.delayservicereactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class DelayServiceReactiveApplication {

    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation(); //for tracing log in reactive
        SpringApplication.run(DelayServiceReactiveApplication.class, args);
    }

}
