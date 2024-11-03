package com.reuven.websocket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(UUID id, String body, LocalDateTime sentAt) {
}