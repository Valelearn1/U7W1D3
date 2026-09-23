package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recensioni")
@Getter
@Setter
@NoArgsConstructor
public class Recensione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Testo scritto dall'utente: e' il campo piu' pericoloso di tutta l'app,
    // perche' entra da fuori e finisce dritto in pagina. Vedi RecensioneService.
    @Column(nullable = false, length = 4000)
    private String testo;

    @Column(nullable = false)
    private Integer voto;

    @Column(nullable = false)
    private LocalDateTime creataIl = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prodotto_id", nullable = false)
    private Prodotto prodotto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autore_id", nullable = false)
    private Utente autore;

    public Recensione(String testo, Integer voto, Prodotto prodotto, Utente autore) {
        this.testo = testo;
        this.voto = voto;
        this.prodotto = prodotto;
        this.autore = autore;
    }
}
