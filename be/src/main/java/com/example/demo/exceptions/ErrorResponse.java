package com.example.demo.exceptions;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(String messaggio, LocalDateTime timestamp, List<String> dettagli) {
    public ErrorResponse(String messaggio) {
        this(messaggio, LocalDateTime.now(), List.of());
    }
}
