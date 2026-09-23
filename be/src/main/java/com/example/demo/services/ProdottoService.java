package com.example.demo.services;

import com.example.demo.config.VulnerabilityFlags;
import com.example.demo.dto.ProdottoAdminDTO;
import com.example.demo.dto.ProdottoDTO;
import com.example.demo.dto.ProdottoResponseDTO;
import com.example.demo.entities.Prodotto;
import com.example.demo.entities.Ruolo;
import com.example.demo.entities.Utente;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repositories.ProdottoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProdottoService {

    private final ProdottoRepository prodottoRepository;
    private final VulnerabilityFlags flags;

    @PersistenceContext
    private EntityManager entityManager;

    public ProdottoService(ProdottoRepository prodottoRepository, VulnerabilityFlags flags) {
        this.prodottoRepository = prodottoRepository;
        this.flags = flags;
    }

    // ==========================================================================
    //  TRE RISPOSTE DIVERSE ALLO STESSO INDIRIZZO (consegna D2)
    //  E' il SERVER a decidere cosa entra nel JSON, non la pagina:
    //   - anonimo / USER -> solo articoli PUBBLICATI, solo campi pubblici (ProdottoResponseDTO)
    //   - ADMIN          -> anche le BOZZE, e i campi riservati (ProdottoAdminDTO)
    // ==========================================================================

    private boolean isAdmin(Utente richiedente) {
        return richiedente != null && richiedente.getRuolo() == Ruolo.ADMIN;
    }

    /** La lista del catalogo, modellata sul ruolo di chi la chiede. */
    public List<Object> catalogo(Utente richiedente) {
        if (isAdmin(richiedente)) {
            // ADMIN: tutto, bozze comprese, con i campi riservati.
            return prodottoRepository.findAll().stream().map(this::toAdminDTO).map(o -> (Object) o).toList();
        }
        // Pubblico: SOLO i pubblicati, e solo i campi pubblici.
        // Il filtro sulle righe lo fa il DB (findByPubblicatoTrue), non il frontend.
        return prodottoRepository.findByPubblicatoTrue().stream().map(this::toPublicDTO).map(o -> (Object) o).toList();
    }

    /** Un singolo articolo, modellato sul ruolo. Una bozza non esiste per il pubblico (404). */
    public Object dettaglio(Long id, Utente richiedente) {
        Prodotto p = findEntityById(id);
        if (isAdmin(richiedente)) {
            return toAdminDTO(p);
        }
        // Se non e' pubblicato, per il pubblico e' come se non ci fosse: 404, non 403.
        if (!p.isPubblicato()) {
            throw new NotFoundException(id);
        }
        return toPublicDTO(p);
    }

    // ==========================================================================
    //  IL PUNTO DEBOLE N.1 - SQL INJECTION
    //  Bersaglio dell'Agente SQLi.
    // ==========================================================================
    @SuppressWarnings("unchecked")
    public List<Object> cerca(String q, Utente richiedente) {
        List<Prodotto> trovati;

        if (flags.isSqli()) {
            // ===== VERSIONE VULNERABILE =====
            // La stringa che arriva dall'utente viene CONCATENATA dentro il testo della query.
            // Per il database non esiste piu' differenza fra "istruzione" e "dato": tutto e'
            // un unico testo SQL da interpretare. Chi scrive la q decide cosa esegue il DB.
            //
            // Con q = "%' OR '1'='1" la condizione diventa sempre vera: torna l'intero
            // catalogo invece dei match. Lo stesso buco, con UNION SELECT, ruba la tabella utenti.
            String sql = "SELECT * FROM prodotti WHERE nome ILIKE '%" + q + "%'";
            trovati = entityManager.createNativeQuery(sql, Prodotto.class).getResultList();
        } else {
            // ===== VERSIONE SICURA =====
            // Query parametrizzata: il valore di q viaggia SEPARATO dal testo della query,
            // il DB lo tratta come dato qualunque cosa contenga. Non serve "ripulire" l'input,
            // serve non concatenarlo.
            trovati = prodottoRepository.findByNomeContainingIgnoreCase(q);
        }

        // Anche la ricerca rispetta i tre livelli: il pubblico non vede le bozze fra i risultati.
        if (isAdmin(richiedente)) {
            return trovati.stream().map(this::toAdminDTO).map(o -> (Object) o).toList();
        }
        return trovati.stream()
                .filter(Prodotto::isPubblicato)
                .map(this::toPublicDTO).map(o -> (Object) o).toList();
    }

    public Prodotto findEntityById(Long id) {
        return prodottoRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    // --- Scritture: solo ADMIN (protette da @PreAuthorize nel controller) ---

    public ProdottoAdminDTO crea(ProdottoDTO body) {
        Prodotto p = new Prodotto(body.nome(), body.descrizione(), body.prezzo(), body.categoria());
        p.setPubblicato(Boolean.TRUE.equals(body.pubblicato())); // se non specificato -> BOZZA
        p.setPrezzoAcquisto(body.prezzoAcquisto());
        p.setFornitore(body.fornitore());
        return toAdminDTO(prodottoRepository.save(p));
    }

    public ProdottoAdminDTO aggiorna(Long id, ProdottoDTO body) {
        Prodotto p = findEntityById(id);
        p.setNome(body.nome());
        p.setDescrizione(body.descrizione());
        p.setPrezzo(body.prezzo());
        p.setCategoria(body.categoria());
        if (body.pubblicato() != null) p.setPubblicato(body.pubblicato());
        if (body.prezzoAcquisto() != null) p.setPrezzoAcquisto(body.prezzoAcquisto());
        if (body.fornitore() != null) p.setFornitore(body.fornitore());
        return toAdminDTO(prodottoRepository.save(p));
    }

    /** Pubblica una bozza: la rende visibile al pubblico. */
    public ProdottoAdminDTO pubblica(Long id) {
        Prodotto p = findEntityById(id);
        p.setPubblicato(true);
        return toAdminDTO(prodottoRepository.save(p));
    }

    public ProdottoAdminDTO cambiaPrezzo(Long id, BigDecimal nuovoPrezzo) {
        Prodotto p = findEntityById(id);
        p.setPrezzo(nuovoPrezzo);
        return toAdminDTO(prodottoRepository.save(p));
    }

    public void elimina(Long id) {
        prodottoRepository.delete(findEntityById(id));
    }

    // --- Mappatura: due forme diverse dello stesso articolo ---

    private ProdottoResponseDTO toPublicDTO(Prodotto p) {
        return new ProdottoResponseDTO(p.getId(), p.getNome(), p.getDescrizione(), p.getPrezzo(), p.getCategoria());
    }

    private ProdottoAdminDTO toAdminDTO(Prodotto p) {
        BigDecimal margine = (p.getPrezzo() != null && p.getPrezzoAcquisto() != null)
                ? p.getPrezzo().subtract(p.getPrezzoAcquisto())
                : null;
        return new ProdottoAdminDTO(
                p.getId(), p.getNome(), p.getDescrizione(), p.getPrezzo(), p.getCategoria(),
                p.isPubblicato(), p.getPrezzoAcquisto(), p.getFornitore(), margine);
    }
}
