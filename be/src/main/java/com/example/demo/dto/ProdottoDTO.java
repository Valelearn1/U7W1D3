package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// Payload di creazione/modifica prodotto (solo ADMIN).
// I campi riservati e lo stato sono opzionali: se assenti, l'articolo nasce come BOZZA.
public record ProdottoDTO(
        @NotBlank String nome,
        String descrizione,
        @NotNull @PositiveOrZero BigDecimal prezzo,
        @NotBlank String categoria,
        Boolean pubblicato,
        @PositiveOrZero BigDecimal prezzoAcquisto,
        String fornitore
) {}
