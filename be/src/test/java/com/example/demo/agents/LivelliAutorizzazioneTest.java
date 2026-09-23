package com.example.demo.agents;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica i TRE LIVELLI dell'autorizzazione (consegna D2).
 * A differenza degli agenti, questo non cerca vulnerabilita': controlla che ogni livello
 * protegga davvero il suo indirizzo. E' la prova a supporto della relazione.
 *
 * Il LIVELLO 3 (ownership) e' gia' coperto in dettaglio da IdorAgentTest.
 */
class LivelliAutorizzazioneTest extends AgentBase {

    @Test
    @DisplayName("Livello 1 — la catena dei filtri: vetrina pubblica, preferiti solo se loggato")
    void livello1_catenaFiltri() {
        // La vetrina e' pubblica: nessun token, risposta 200.
        assertEquals(200, get("/api/prodotti", null).statusCode(),
                "La vetrina /api/prodotti deve essere pubblica");

        // I preferiti pretendono un utente collegato: senza token il filtro blocca con 401.
        assertEquals(401, get("/api/preferiti", null).statusCode(),
                "Senza token /api/preferiti deve dare 401 (lo decide il filtro, non il controller)");

        // Con un token valido si passa il livello 1.
        assertEquals(200, get("/api/preferiti", tokenUser).statusCode(),
                "Con token valido /api/preferiti deve rispondere");
    }

    @Test
    @DisplayName("Livello 2 — le annotazioni: pubblicare un prodotto spetta all'ADMIN")
    void livello2_preAuthorize() {
        Map<String, Object> nuovo = Map.of("nome", "Prova", "prezzo", 10, "categoria", "Test");

        // Un USER e' autenticato (supera il livello 1) ma non ha il ruolo: 403 da @PreAuthorize.
        HttpResponse<String> daUser = post("/api/prodotti", nuovo, tokenUser);
        assertEquals(403, daUser.statusCode(),
                "Un USER non deve poter creare prodotti (lo decide @PreAuthorize, non il filtro)");

        // Lo stesso identico indirizzo, con un ADMIN, funziona.
        HttpResponse<String> daAdmin = post("/api/prodotti", nuovo, tokenAdmin);
        assertEquals(201, daAdmin.statusCode(),
                "Un ADMIN deve poter creare prodotti");
    }
}
