package com.example.demo.config;

import java.net.URI;

/**
 * Render pubblica l'indirizzo del database in una sola variabile, DATABASE_URL, e lo fa
 * nel formato di libreria di PostgreSQL:
 *
 *     postgresql://utente:password@host:5432/nomedb
 *
 * Il driver JDBC di Java quel formato non lo capisce: vuole un URL che comincia per
 * "jdbc:postgresql://" e vuole utente e password come parametri separati. Questa classe fa
 * la traduzione e scrive il risultato in tre variabili di sistema che application.yml legge
 * come DB_URL, DB_USERNAME, DB_PASSWORD.
 *
 * Va chiamata come PRIMA riga del main: Spring legge la configurazione appena parte, quindi
 * se la traduzione arriva dopo, l'applicazione sta gia' cercando il database all'indirizzo
 * sbagliato. In locale DATABASE_URL non esiste e il metodo non fa niente: restano validi
 * i valori di ripiego scritti in application.yml.
 */
public final class DatabaseUrl {

    private DatabaseUrl() {
    }

    public static void applica() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return; // in locale non c'e': si usano i valori di application.yml
        }

        URI uri = URI.create(databaseUrl);

        String[] credenziali = uri.getUserInfo().split(":", 2);
        String utente = credenziali[0];
        String password = credenziali.length > 1 ? credenziali[1] : "";

        int porta = uri.getPort() == -1 ? 5432 : uri.getPort();
        // sslmode=require: Render rifiuta le connessioni in chiaro verso il database.
        String jdbc = "jdbc:postgresql://%s:%d%s?sslmode=require".formatted(uri.getHost(), porta, uri.getPath());

        System.setProperty("DB_URL", jdbc);
        System.setProperty("DB_USERNAME", utente);
        System.setProperty("DB_PASSWORD", password);
    }
}
