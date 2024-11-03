package com.reuven.delayservicereactive.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class DelayController {

    private static final Logger logger = LoggerFactory.getLogger(DelayController.class);

    private static final Duration DELAY_DURATION = Duration.ofMillis(200);

    @GetMapping("/delay/{sessionId}")
    public Mono<String> delay(@PathVariable String sessionId) {
        return Mono.delay(DELAY_DURATION)
                .map(message -> String.format("SessionId %s. delayed response at %s for %s", sessionId, LocalDateTime.now(), DELAY_DURATION))
                .onErrorResume(e -> {
                    logger.error("Error occurred: {}", e.getMessage());
                    return Mono.empty();
                })
                .doOnNext(logger::info);
    }
}
