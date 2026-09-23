package com.example.demo.controllers;

import com.example.demo.dto.ProdottoAdminDTO;
import com.example.demo.services.ProdottoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * La superficie "legacy": si autentica con il cookie accessToken (vedi AuthFilter).
 * Esiste per avere un bersaglio realistico per l'Agente CSRF, perche' e' l'unico posto
 * dove il browser allega la credenziale da solo.
 */
@RestController
@RequestMapping("/api/legacy")
public class LegacyController {

    private final ProdottoService prodottoService;

    public LegacyController(ProdottoService prodottoService) {
        this.prodottoService = prodottoService;
    }

    /**
     * Il client chiama questo endpoint per farsi dare il token CSRF prima di una scrittura.
     * Chiedere CsrfToken come parametro non e' un dettaglio: e' cio' che FORZA Spring a
     * generare il token e a depositare il cookie XSRF-TOKEN nella risposta.
     * Quando la protezione e' disattivata il parametro arriva null, e lo diciamo apertamente.
     */
    @GetMapping("/csrf")
    public Map<String, String> csrfToken(CsrfToken token) {
        if (token == null) {
            return Map.of("stato", "PROTEZIONE CSRF DISATTIVATA", "token", "");
        }
        return Map.of("stato", "attiva", "headerName", token.getHeaderName(), "token", token.getToken());
    }

    /** Scrittura sensibile: cambia il prezzo di un prodotto. Solo ADMIN. */
    @PostMapping("/prodotti/{id}/prezzo")
    @PreAuthorize("hasRole('ADMIN')")
    public ProdottoAdminDTO cambiaPrezzo(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        return prodottoService.cambiaPrezzo(id, body.get("prezzo"));
    }
}
