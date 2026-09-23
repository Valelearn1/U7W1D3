package com.example.demo.repositories;

import com.example.demo.entities.Prodotto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdottoRepository extends JpaRepository<Prodotto, Long> {

    // VERSIONE SICURA della ricerca: la query e' scritta una volta sola e il valore
    // dell'utente viaggia come PARAMETRO (?1), mai come pezzo di stringa SQL.
    // Il driver lo manda al DB separato dalla query, quindi non puo' cambiarne il significato.
    List<Prodotto> findByNomeContainingIgnoreCase(String frammento);

    // Solo gli articoli pubblicati: e' la lista che vede il PUBBLICO.
    // Filtro a livello di riga fatto dal DB, non dal frontend.
    List<Prodotto> findByPubblicatoTrue();
}
