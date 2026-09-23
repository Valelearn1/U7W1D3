package com.example.demo.dto;

import java.math.BigDecimal;

public record PreferitoResponseDTO(Long id, Long prodottoId, String nomeProdotto, BigDecimal prezzo) {}
