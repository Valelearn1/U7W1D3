package com.example.demo.repositories;

import com.example.demo.entities.Preferito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreferitoRepository extends JpaRepository<Preferito, Long> {

    // LIVELLO 3 — la lettura ristretta per proprietario.
    // Nota che qui non esiste un semplice "trovami il preferito con questo id":
    // il metodo giusto pretende SEMPRE anche il proprietario. E' il modo di rendere
    // difficile scrivere per sbaglio la query vulnerabile.
    List<Preferito> findByProprietarioId(Long proprietarioId);

    // La ricerca "sicura": id + proprietario insieme. Se il preferito non e' tuo,
    // torna Optional.empty() esattamente come se non esistesse (-> 404, non 403:
    // non riveliamo nemmeno che quel preferito esiste).
    Optional<Preferito> findByIdAndProprietarioId(Long id, Long proprietarioId);

    boolean existsByProprietarioIdAndProdottoId(Long proprietarioId, Long prodottoId);
}
