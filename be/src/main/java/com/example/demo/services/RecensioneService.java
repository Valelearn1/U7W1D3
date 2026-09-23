package com.example.demo.services;

import com.example.demo.config.VulnerabilityFlags;
import com.example.demo.dto.RecensioneDTO;
import com.example.demo.dto.RecensioneResponseDTO;
import com.example.demo.entities.Prodotto;
import com.example.demo.entities.Recensione;
import com.example.demo.entities.Utente;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repositories.RecensioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class RecensioneService {

    private final RecensioneRepository recensioneRepository;
    private final ProdottoService prodottoService;
    private final VulnerabilityFlags flags;

    public RecensioneService(RecensioneRepository recensioneRepository,
                             ProdottoService prodottoService,
                             VulnerabilityFlags flags) {
        this.recensioneRepository = recensioneRepository;
        this.prodottoService = prodottoService;
        this.flags = flags;
    }

    // ==========================================================================
    //  IL PUNTO DEBOLE N.2 - XSS STORED
    //  Bersaglio dell'Agente XSS.
    // ==========================================================================
    public RecensioneResponseDTO aggiungi(Long prodottoId, RecensioneDTO body, Utente autore) {
        Prodotto prodotto = prodottoService.findEntityById(prodottoId);

        String testo;
        if (flags.isXss()) {
            // ===== VERSIONE VULNERABILE =====
            // Il testo viene salvato ESATTAMENTE come arriva. Se contiene
            // <img src=x onerror=alert(1)>, quel markup resta nel database e viene
            // riservito a ogni visitatore che apre la pagina del prodotto.
            // Si chiama STORED XSS perche' l'attacco e' persistente: basta scriverlo una volta
            // e colpisce chiunque passi di li'. Con una sessione in mano puo' rubare token,
            // fare richieste a nome della vittima, riscrivere la pagina.
            testo = body.testo();
        } else {
            // ===== VERSIONE SICURA =====
            // htmlEscape trasforma i caratteri che hanno un significato in HTML nelle loro
            // entita': < diventa &lt;, > diventa &gt;, " diventa &quot;.
            // Il browser le mostra come testo e non le interpreta piu' come tag.
            // Nota che NON stiamo "togliendo" nulla: la recensione resta leggibile identica,
            // cambia solo che smette di essere codice eseguibile.
            testo = HtmlUtils.htmlEscape(body.testo());
        }

        Recensione recensione = new Recensione(testo, body.voto(), prodotto, autore);
        return toDTO(recensioneRepository.save(recensione));
    }

    public List<RecensioneResponseDTO> findByProdotto(Long prodottoId) {
        prodottoService.findEntityById(prodottoId); // 404 se il prodotto non esiste
        return recensioneRepository.findByProdottoId(prodottoId).stream().map(this::toDTO).toList();
    }

    public void elimina(Long id) {
        Recensione r = recensioneRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
        recensioneRepository.delete(r);
    }

    private RecensioneResponseDTO toDTO(Recensione r) {
        return new RecensioneResponseDTO(
                r.getId(), r.getTesto(), r.getVoto(), r.getAutore().getNome(), r.getCreataIl());
    }
}
