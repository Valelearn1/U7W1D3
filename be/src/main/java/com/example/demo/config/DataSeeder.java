package com.example.demo.config;

import com.example.demo.entities.Prodotto;
import com.example.demo.entities.Ruolo;
import com.example.demo.entities.Utente;
import com.example.demo.repositories.ProdottoRepository;
import com.example.demo.repositories.UtenteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Popola il database al primo avvio, cosi' gli agenti trovano qualcosa da interrogare. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UtenteRepository utenteRepository;
    private final ProdottoRepository prodottoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UtenteRepository utenteRepository, ProdottoRepository prodottoRepository,
                      PasswordEncoder passwordEncoder) {
        this.utenteRepository = utenteRepository;
        this.prodottoRepository = prodottoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (utenteRepository.count() == 0) {
            utenteRepository.save(new Utente("admin@demo.it", passwordEncoder.encode("adminadmin"), "Admin", Ruolo.ADMIN));
            utenteRepository.save(new Utente("user@demo.it", passwordEncoder.encode("useruser12"), "Utente Normale", Ruolo.USER));
            // Secondo utente normale: serve alla demo IDOR (un utente prova a toccare i preferiti dell'altro).
            utenteRepository.save(new Utente("altro@demo.it", passwordEncoder.encode("useruser12"), "Altro Utente", Ruolo.USER));
            System.out.println(">> Utenti di test creati: admin@demo.it - user@demo.it - altro@demo.it");
        }

        if (prodottoRepository.count() == 0) {
            // Costruttore completo: (nome, descrizione, prezzoVendita, categoria,
            //                        pubblicato, prezzoAcquisto, fornitore)
            // Il "descrizione" tiene la specie / nota di cura. I "pubblicato = false"
            // sono le piante ancora IN SERRA: le vede solo l'ADMIN.
            prodottoRepository.save(new Prodotto("Monstera Deliciosa", "Foglie ampie, luce indiretta", new BigDecimal("24.90"), "Da interno", true, new BigDecimal("11.00"), "Verde Vivo"));
            prodottoRepository.save(new Prodotto("Ficus Lyrata", "Ficus dalla foglia a violino", new BigDecimal("34.00"), "Da interno", true, new BigDecimal("16.00"), "Verde Vivo"));
            prodottoRepository.save(new Prodotto("Pothos Aureo", "Ricadente, molto resistente", new BigDecimal("12.50"), "Da interno", true, new BigDecimal("4.50"), "FogliaVerde"));
            prodottoRepository.save(new Prodotto("Sansevieria", "Lingua di suocera, poca acqua", new BigDecimal("18.00"), "Da interno", true, new BigDecimal("7.00"), "FogliaVerde"));
            prodottoRepository.save(new Prodotto("Cactus Echinopsis", "Cactus globoso, pieno sole", new BigDecimal("9.90"), "Grasse", true, new BigDecimal("3.00"), "DesertoCasa"));
            prodottoRepository.save(new Prodotto("Aloe Vera", "Succulenta officinale", new BigDecimal("11.00"), "Grasse", true, new BigDecimal("3.50"), "DesertoCasa"));
            prodottoRepository.save(new Prodotto("Echeveria", "Rosetta carnosa, sole diretto", new BigDecimal("7.50"), "Grasse", true, new BigDecimal("2.20"), "DesertoCasa"));
            prodottoRepository.save(new Prodotto("Lavanda", "Profumata, ottima in vaso", new BigDecimal("8.90"), "Aromatiche", true, new BigDecimal("3.00"), "OrtoBio"));
            prodottoRepository.save(new Prodotto("Rosmarino", "Aromatica sempreverde", new BigDecimal("6.50"), "Aromatiche", true, new BigDecimal("2.00"), "OrtoBio"));
            prodottoRepository.save(new Prodotto("Basilico Genovese", "Aromatica da balcone", new BigDecimal("4.90"), "Aromatiche", true, new BigDecimal("1.20"), "OrtoBio"));
            prodottoRepository.save(new Prodotto("Orchidea Phalaenopsis", "Fioritura lunga, luce diffusa", new BigDecimal("19.90"), "Fiorite", true, new BigDecimal("9.00"), "FioriPregiati"));
            prodottoRepository.save(new Prodotto("Geranio", "Classico da balcone, fiorito", new BigDecimal("7.90"), "Fiorite", true, new BigDecimal("2.50"), "FioriPregiati"));
            prodottoRepository.save(new Prodotto("Ortensia", "Grandi infiorescenze estive", new BigDecimal("15.00"), "Fiorite", true, new BigDecimal("6.50"), "FioriPregiati"));
            prodottoRepository.save(new Prodotto("Acero Giapponese", "Foglia rossa, da esterno", new BigDecimal("45.00"), "Da esterno", true, new BigDecimal("26.00"), "GiardinoVivo"));
            prodottoRepository.save(new Prodotto("Ulivo in vaso", "Sempreverde mediterraneo", new BigDecimal("39.00"), "Da esterno", true, new BigDecimal("22.00"), "GiardinoVivo"));
            prodottoRepository.save(new Prodotto("Canapa ornamentale", "Cannabis sativa, foglia palmata decorativa", new BigDecimal("14.90"), "Da esterno", true, new BigDecimal("6.50"), "GiardinoVivo"));
            // --- IN SERRA: piante non ancora pubblicate (le vede solo l'ADMIN) ---
            prodottoRepository.save(new Prodotto("Bonsai Ginseng", "In coltivazione, in arrivo", new BigDecimal("29.00"), "Da interno", false, new BigDecimal("15.00"), "Verde Vivo"));
            prodottoRepository.save(new Prodotto("Monstera Variegata", "Rara, in propagazione", new BigDecimal("89.00"), "Da interno", false, new BigDecimal("55.00"), "Verde Vivo"));
            prodottoRepository.save(new Prodotto("Dionaea (pianta carnivora)", "Preordine, in germinazione", new BigDecimal("16.90"), "Da interno", false, new BigDecimal("8.00"), "OrtoBio"));
            System.out.println(">> Catalogo di test creato: 19 piante (16 pubblicate + 3 in serra)");
        }
    }
}
