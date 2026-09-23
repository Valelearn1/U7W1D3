package com.example.demo.dto;

import java.math.BigDecimal;

public record ProdottoResponseDTO(Long id, String nome, String descrizione, BigDecimal prezzo, String categoria) {}
