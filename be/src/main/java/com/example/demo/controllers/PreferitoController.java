package com.example.demo.controllers;

import com.example.demo.dto.PreferitoResponseDTO;
import com.example.demo.entities.Utente;
import com.example.demo.services.PreferitoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * I preferiti dell'utente. Tutta questa risorsa e' il banco di prova del LIVELLO 3.
 *
 * Notare cosa NON c'e' qui: nessun @PreAuthorize. Non serve un ruolo per gestire i
 * propri preferiti — serve possederli. Chi decide "quali righe sono tue" e' la query
 * dentro il service, non un'annotazione.
 */
@RestController
@RequestMapping("/api/preferiti")
public class PreferitoController {

    private final PreferitoService preferitoService;

    public PreferitoController(PreferitoService preferitoService) {
        this.preferitoService = preferitoService;
    }

    // LIVELLO 1 in azione: senza JWT il filtro blocca qui con 401.
    // LIVELLO 3 in azione: la lista e' quella del richiedente, non "tutti i preferiti".
    @GetMapping
    public List<PreferitoResponseDTO> mieiPreferiti(@AuthenticationPrincipal Utente utente) {
        return preferitoService.mieiPreferiti(utente);
    }

    @PostMapping("/{prodottoId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PreferitoResponseDTO aggiungi(@PathVariable Long prodottoId,
                                         @AuthenticationPrincipal Utente utente) {
        return preferitoService.aggiungi(prodottoId, utente);
    }

    // LIVELLO 3 puro: l'id nell'URL e' controllato dal client, quindi qualsiasi utente
    // puo' provare a leggere il preferito di un altro. E' la query a difendere.
    @GetMapping("/{id}")
    public PreferitoResponseDTO trova(@PathVariable Long id, @AuthenticationPrincipal Utente utente) {
        return preferitoService.trova(id, utente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long id, @AuthenticationPrincipal Utente utente) {
        preferitoService.elimina(id, utente);
    }
}
