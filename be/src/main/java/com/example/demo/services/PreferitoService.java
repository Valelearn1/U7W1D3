package com.example.demo.services;

import com.example.demo.config.VulnerabilityFlags;
import com.example.demo.dto.PreferitoResponseDTO;
import com.example.demo.entities.Preferito;
import com.example.demo.entities.Prodotto;
import com.example.demo.entities.Utente;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repositories.PreferitoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PreferitoService {

    private final PreferitoRepository preferitoRepository;
    private final ProdottoService prodottoService;
    private final VulnerabilityFlags flags;

    public PreferitoService(PreferitoRepository preferitoRepository,
                            ProdottoService prodottoService,
                            VulnerabilityFlags flags) {
        this.preferitoRepository = preferitoRepository;
        this.prodottoService = prodottoService;
        this.flags = flags;
    }

    // I miei preferiti: la lista e' SEMPRE ristretta al proprietario corrente.
    // Qui non c'e' una versione vulnerabile: la query parte gia' dal proprietario,
    // non da un id arbitrario, quindi non c'e' modo di vedere quelli di un altro.
    public List<PreferitoResponseDTO> mieiPreferiti(Utente proprietario) {
        return preferitoRepository.findByProprietarioId(proprietario.getId()).stream()
                .map(this::toDTO).toList();
    }

    public PreferitoResponseDTO aggiungi(Long prodottoId, Utente proprietario) {
        if (preferitoRepository.existsByProprietarioIdAndProdottoId(proprietario.getId(), prodottoId)) {
            throw new BadRequestException("Prodotto gia' tra i tuoi preferiti");
        }
        Prodotto prodotto = prodottoService.findEntityById(prodottoId);
        return toDTO(preferitoRepository.save(new Preferito(proprietario, prodotto)));
    }

    // ==========================================================================
    //  IL PUNTO DEBOLE N.4 - IDOR (Broken Object-Level Authorization)
    //  E' il LIVELLO 3 dell'autorizzazione: quello che ne' il filtro ne'
    //  @PreAuthorize possono coprire, perche' dipende da CHI possiede la riga.
    //  Bersaglio dell'Agente IDOR.
    // ==========================================================================

    // Leggere un singolo preferito tramite id.
    public PreferitoResponseDTO trova(Long id, Utente richiedente) {
        return toDTO(caricaControllandoProprieta(id, richiedente));
    }

    // Cancellare un preferito tramite id.
    public void elimina(Long id, Utente richiedente) {
        preferitoRepository.delete(caricaControllandoProprieta(id, richiedente));
    }

    /**
     * Il metodo dove vive la differenza tra le due versioni.
     *
     * Il filtro (livello 1) ha gia' verificato che il richiedente e' loggato.
     * Non c'e' @PreAuthorize (livello 2): gestire i propri preferiti e' cosa da
     * qualsiasi utente autenticato, non serve un ruolo speciale.
     * Ma proprio per questo l'UNICO controllo che decide se PUOI toccare QUESTA riga
     * e' la query: e' il livello 3.
     */
    private Preferito caricaControllandoProprieta(Long id, Utente richiedente) {
        if (flags.isIdor()) {
            // ===== VERSIONE VULNERABILE =====
            // Cerco per solo id, senza guardare il proprietario. Il preferito viene
            // restituito a chiunque sia loggato, anche se e' di un altro utente.
            // Cambiando il numero nell'URL (/api/preferiti/7) leggo o cancello il
            // preferito #7 di chiunque: e' l'IDOR. Il ruolo qui non aiuta, perche' non
            // e' una questione di "cosa puoi fare" ma di "su quale riga".
            return preferitoRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
        }

        // ===== VERSIONE SICURA =====
        // Cerco per id E proprietario nella STESSA query. Se il preferito non e' mio,
        // il risultato e' vuoto e rispondo 404, identico a "non esiste": non lascio
        // nemmeno capire che quel preferito c'e'. La difesa e' nel dato, non nel ruolo.
        return preferitoRepository.findByIdAndProprietarioId(id, richiedente.getId())
                .orElseThrow(() -> new NotFoundException(id));
    }

    private PreferitoResponseDTO toDTO(Preferito p) {
        return new PreferitoResponseDTO(
                p.getId(), p.getProdotto().getId(), p.getProdotto().getNome(), p.getProdotto().getPrezzo());
    }
}
