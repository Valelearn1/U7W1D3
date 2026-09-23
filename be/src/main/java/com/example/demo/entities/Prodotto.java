package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prodotti")
@Getter
@Setter
@NoArgsConstructor
public class Prodotto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 2000)
    private String descrizione;

    // Prezzo di VENDITA: campo pubblico, lo vedono tutti.
    @Column(nullable = false)
    private BigDecimal prezzo;

    @Column(nullable = false)
    private String categoria;

    // --- Stato pubblicato/bozza -------------------------------------------------
    // false = BOZZA: la vedono solo gli ADMIN. Il pubblico non deve nemmeno sapere
    // che esiste. E' un filtro a livello di RIGA (quali articoli), deciso dal server.
    @Column(nullable = false)
    private boolean pubblicato = false;

    // --- Campi RISERVATI: solo gli ADMIN li vedono -----------------------------
    // Sono un filtro a livello di CAMPO: lo stesso articolo, ma con piu' colonne
    // visibili a seconda del ruolo. Il pubblico non deve conoscere quanto ci costa
    // un articolo ne' da chi lo compriamo.
    @Column(precision = 10, scale = 2)
    private BigDecimal prezzoAcquisto;

    private String fornitore;

    @OneToMany(mappedBy = "prodotto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recensione> recensioni = new ArrayList<>();

    // Costruttore breve: crea una BOZZA (pubblicato = false), senza dati riservati.
    public Prodotto(String nome, String descrizione, BigDecimal prezzo, String categoria) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.prezzo = prezzo;
        this.categoria = categoria;
    }

    // Costruttore completo: usato dal seeder per popolare anche stato e campi riservati.
    public Prodotto(String nome, String descrizione, BigDecimal prezzo, String categoria,
                    boolean pubblicato, BigDecimal prezzoAcquisto, String fornitore) {
        this(nome, descrizione, prezzo, categoria);
        this.pubblicato = pubblicato;
        this.prezzoAcquisto = prezzoAcquisto;
        this.fornitore = fornitore;
    }
}
