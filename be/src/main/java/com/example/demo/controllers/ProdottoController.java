package com.example.demo.controllers;

import com.example.demo.dto.ProdottoAdminDTO;
import com.example.demo.dto.ProdottoDTO;
import com.example.demo.entities.Utente;
import com.example.demo.services.ProdottoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prodotti")
public class ProdottoController {

    private final ProdottoService prodottoService;

    public ProdottoController(ProdottoService prodottoService) {
        this.prodottoService = prodottoService;
    }

    // Il principal arriva NULL se la richiesta e' anonima, valorizzato se c'e' un token.
    // Per questo il filtro fa "autenticazione opzionale" su questi GET: se l'admin manda
    // il token viene riconosciuto e riceve la vista completa; altrimenti resta pubblico.

    @GetMapping
    public List<Object> catalogo(@AuthenticationPrincipal Utente utente) {
        return prodottoService.catalogo(utente);
    }

    /** Ricerca pubblica. E' l'endpoint che l'Agente SQLi prende di mira. */
    @GetMapping("/cerca")
    public List<Object> cerca(@RequestParam(defaultValue = "") String q,
                              @AuthenticationPrincipal Utente utente) {
        return prodottoService.cerca(q, utente);
    }

    @GetMapping("/{id}")
    public Object findById(@PathVariable Long id, @AuthenticationPrincipal Utente utente) {
        return prodottoService.dettaglio(id, utente);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProdottoAdminDTO crea(@RequestBody @Valid ProdottoDTO body) {
        return prodottoService.crea(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProdottoAdminDTO aggiorna(@PathVariable Long id, @RequestBody @Valid ProdottoDTO body) {
        return prodottoService.aggiorna(id, body);
    }

    /** Pubblica una bozza. Operazione da ADMIN (livello 2). */
    @PatchMapping("/{id}/pubblica")
    @PreAuthorize("hasRole('ADMIN')")
    public ProdottoAdminDTO pubblica(@PathVariable Long id) {
        return prodottoService.pubblica(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void elimina(@PathVariable Long id) {
        prodottoService.elimina(id);
    }
}
