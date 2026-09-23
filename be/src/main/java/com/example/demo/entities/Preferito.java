package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un prodotto che un utente ha messo tra i preferiti.
// E' una risorsa "di proprieta'": ogni riga appartiene a UN utente preciso.
// E' su questa proprieta' che agisce il LIVELLO 3 dell'autorizzazione (vedi PreferitoService).
@Entity
@Table(name = "preferiti",
        // Un utente non puo' avere lo stesso prodotto due volte tra i preferiti.
        uniqueConstraints = @UniqueConstraint(columnNames = {"proprietario_id", "prodotto_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Preferito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Il proprietario: chi ha salvato il preferito. E' il campo su cui si restringe la query.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Utente proprietario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    public Preferito(Utente proprietario, Prodotto prodotto) {
        this.proprietario = proprietario;
        this.prodotto = prodotto;
    }
}
