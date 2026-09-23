package com.example.demo.agents;

import com.example.demo.agents.report.Esito;
import com.example.demo.agents.report.Finding;
import com.example.demo.agents.report.ReportWriter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * AGENTE IDOR — verifica il LIVELLO 3 dell'autorizzazione (consegna D2).
 *
 * Domanda a cui risponde: "un utente puo' toccare i dati di un ALTRO utente cambiando
 * l'id nell'URL?" E' l'unico livello che ne' la catena dei filtri ne' @PreAuthorize
 * possono coprire, perche' non dipende dal ruolo ma da chi possiede la riga.
 *
 * Scenario: l'utente VITTIMA salva un preferito. L'utente ATTACCANTE (un altro utente
 * normale, stesso ruolo) prova a leggerlo e cancellarlo usando il suo id.
 *   - se ci riesce -> IDOR: la query non restringe per proprietario (VULNERABILE)
 *   - se ottiene 404 -> il dato e' protetto a livello di query (SICURO)
 */
class IdorAgentTest extends AgentBase {

    @Test
    @DisplayName("Agente IDOR: un utente prova ad accedere ai preferiti di un altro")
    void testaAccessoIncrociato() {
        List<Finding> findings = new ArrayList<>();

        // La vittima (tokenUser) salva un preferito e ne ottiene l'id.
        // Se e' gia' presente da una run precedente, lo recupero dalla sua lista.
        long idVittima;
        HttpResponse<String> creazione = post("/api/preferiti/1", null, tokenUser);
        if (creazione.statusCode() == 201) {
            idVittima = leggi(creazione.body()).get("id").asLong();
        } else {
            JsonNode lista = leggi(get("/api/preferiti", tokenUser).body());
            if (!lista.isArray() || lista.isEmpty()) {
                throw new IllegalStateException("Impossibile preparare il preferito della vittima: " + estratto(creazione));
            }
            idVittima = lista.get(0).get("id").asLong();
        }

        // --- Attacco 1: LETTURA del preferito altrui ---
        HttpResponse<String> letturaAttacco = get("/api/preferiti/" + idVittima, tokenAltro);
        boolean lettoAltrui = letturaAttacco.statusCode() == 200;
        findings.add(new Finding("Agente IDOR", "/api/preferiti/{id}", "GET",
                "Un utente legge il preferito di un altro cambiando l'id",
                "id vittima = " + idVittima + ", token attaccante",
                lettoAltrui ? Esito.VULNERABILE : (letturaAttacco.statusCode() == 404 ? Esito.SICURO : Esito.DA_VERIFICARE),
                lettoAltrui
                        ? "L'attaccante ha LETTO un preferito che non e' suo: la query non filtra per proprietario. " + estratto(letturaAttacco)
                        : "Accesso negato come se il dato non esistesse (atteso 404). " + estratto(letturaAttacco)));

        // --- Attacco 2: CANCELLAZIONE del preferito altrui ---
        HttpResponse<String> deleteAttacco = delete("/api/preferiti/" + idVittima, tokenAltro);
        boolean cancellatoAltrui = deleteAttacco.statusCode() == 204;
        findings.add(new Finding("Agente IDOR", "/api/preferiti/{id}", "DELETE",
                "Un utente cancella il preferito di un altro cambiando l'id",
                "id vittima = " + idVittima + ", token attaccante",
                cancellatoAltrui ? Esito.VULNERABILE : (deleteAttacco.statusCode() == 404 ? Esito.SICURO : Esito.DA_VERIFICARE),
                cancellatoAltrui
                        ? "L'attaccante ha CANCELLATO un preferito altrui: nessun controllo di proprieta' nella query. " + estratto(deleteAttacco)
                        : "Cancellazione negata (atteso 404): il dato di un altro e' intoccabile. " + estratto(deleteAttacco)));

        // --- Controprova: la vittima sui PROPRI preferiti deve poter agire ---
        HttpResponse<String> letturaLegittima = get("/api/preferiti/" + idVittima, tokenUser);
        findings.add(new Finding("Agente IDOR", "/api/preferiti/{id}", "GET",
                "Controprova: la vittima legge il PROPRIO preferito", "id vittima = " + idVittima + ", token vittima",
                letturaLegittima.statusCode() == 200 ? Esito.SICURO : Esito.DA_VERIFICARE,
                letturaLegittima.statusCode() == 200
                        ? "Sul proprio dato l'accesso funziona: la difesa e' selettiva, non un blocco totale. " + estratto(letturaLegittima)
                        : "La vittima NON riesce ad accedere al proprio preferito: difesa troppo aggressiva. " + estratto(letturaLegittima)));

        ReportWriter.scrivi("Agente IDOR", "report-idor", findings);
    }
}
