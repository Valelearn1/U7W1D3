package com.example.demo.controllers;

import com.example.demo.dto.RecensioneDTO;
import com.example.demo.dto.RecensioneResponseDTO;
import com.example.demo.entities.Utente;
import com.example.demo.services.RecensioneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prodotti/{prodottoId}/recensioni")
public class RecensioneController {

    private final RecensioneService recensioneService;

    public RecensioneController(RecensioneService recensioneService) {
        this.recensioneService = recensioneService;
    }

    /** Lettura pubblica: e' qui che l'Agente XSS va a rileggere quello che ha scritto. */
    @GetMapping
    public List<RecensioneResponseDTO> findByProdotto(@PathVariable Long prodottoId) {
        return recensioneService.findByProdotto(prodottoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecensioneResponseDTO aggiungi(@PathVariable Long prodottoId,
                                          @RequestBody @Valid RecensioneDTO body,
                                          @AuthenticationPrincipal Utente utenteCorrente) {
        return recensioneService.aggiungi(prodottoId, body, utenteCorrente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void elimina(@PathVariable Long prodottoId, @PathVariable Long id) {
        recensioneService.elimina(id);
    }
}
