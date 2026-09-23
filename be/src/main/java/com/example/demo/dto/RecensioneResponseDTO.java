package com.example.demo.dto;

import java.time.LocalDateTime;

public record RecensioneResponseDTO(Long id, String testo, Integer voto, String autore, LocalDateTime creataIl) {}
