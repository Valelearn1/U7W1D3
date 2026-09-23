package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * La vista ADMIN dello stesso articolo: oltre ai campi pubblici, espone lo stato
 * (pubblicato/bozza) e i dati riservati (prezzo d'acquisto, fornitore), piu' il
 * margine calcolato al volo. Il pubblico riceve invece ProdottoResponseDTO, che
 * questi campi non li ha proprio: e' il server a decidere cosa mettere nel JSON.
 */
public record ProdottoAdminDTO(
        Long id,
        String nome,
        String descrizione,
        BigDecimal prezzo,
        String categoria,
        boolean pubblicato,
        BigDecimal prezzoAcquisto,
        String fornitore,
        BigDecimal margine // prezzo di vendita - prezzo d'acquisto
) {}
