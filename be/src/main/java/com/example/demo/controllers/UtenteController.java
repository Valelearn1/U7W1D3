package com.example.demo.controllers;

import com.example.demo.dto.UtenteDTO;
import com.example.demo.entities.Utente;
import com.example.demo.services.UtenteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @GetMapping("/me")
    public UtenteDTO profilo(@AuthenticationPrincipal Utente utenteCorrente) {
        return utenteService.toDTO(utenteCorrente);
    }

    /** L'elenco completo degli utenti e' roba da ADMIN: un USER non deve vedere le email altrui. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtenteDTO> findAll() {
        return utenteService.findAll();
    }
}
