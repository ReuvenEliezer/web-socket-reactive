package com.reuven.websocketreactive.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record Message(UUID id, String body, LocalDateTime sentAt) {
}
