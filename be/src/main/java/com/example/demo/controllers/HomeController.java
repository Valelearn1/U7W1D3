package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * La radice del backend.
 *
 * Questo servizio e' un'API: parla JSON e non ha pagine. Chi ne apre l'indirizzo nel browser
 * pero' si aspetta un sito, e senza questo endpoint riceverebbe il 401 del filtro
 * ("Token mancante"), che sembra un guasto e non lo e'. Meglio dire cosa e' questo indirizzo
 * e indicare quello giusto.
 */
@RestController
public class HomeController {

    /** L'indirizzo della vetrina: la stessa variabile che serve al CORS. */
    @Value("${app.cors.allowed-origin}")
    private String vetrina;

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "servizio", "Radici — API del vivaio",
                "stato", "attivo",
                "vetrina", vetrina,
                "catalogo", "/api/prodotti",
                "salute", "/actuator/health"
        );
    }
}
