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
import java.util.Map;

/**
 * AGENTE XSS
 *
 * Cerca i valori dell'utente che tornano nella risposta senza essere neutralizzati.
 *
 * La logica: scrivo una recensione il cui testo contiene markup pericoloso, poi rileggo
 * le recensioni di quel prodotto. Se ritrovo il markup IDENTICO a come l'ho mandato, l'app
 * lo restituira' come codice eseguibile al prossimo browser che apre la pagina (stored XSS).
 * Se invece lo ritrovo con i caratteri "<" ">" trasformati in &lt; &gt;, l'app lo ha
 * neutralizzato: resta leggibile come testo ma non e' piu' eseguibile.
 *
 * Payload innocuo di proposito: un alert(1) o un onerror che non ruba niente. Serve a
 * dimostrare che il canale e' aperto, non a colpire qualcuno.
 */
class XssAgentTest extends AgentBase {

    private static final List<String> PAYLOAD = List.of(
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert(1)>",
            "\"><svg/onload=alert(1)>"
    );

    @Test
    @DisplayName("Agente XSS: inietta markup in una recensione e verifica l'escaping in lettura")
    void testaRecensioni() {
        List<Finding> findings = new ArrayList<>();
        long prodottoId = primoProdottoId();

        for (String payload : PAYLOAD) {
            // Scrivo la recensione (serve un utente autenticato: il campo autore viene dal token).
            HttpResponse<String> creazione = post(
                    "/api/prodotti/" + prodottoId + "/recensioni",
                    Map.of("testo", payload, "voto", 5),
                    tokenUser);

            if (creazione.statusCode() >= 400) {
                findings.add(new Finding("Agente XSS", "/api/prodotti/{id}/recensioni", "POST",
                        "Invio recensione con markup", payload, Esito.DA_VERIFICARE,
                        "La creazione e' stata rifiutata, impossibile testare la lettura. " + estratto(creazione)));
                continue;
            }

            // Verifico come il server ha SALVATO questa specifica recensione: il corpo della
            // risposta alla POST e' la serializzazione dell'entita' appena persistita (toDTO
            // dopo il save), quindi riflette esattamente cio' che finira' in pagina per questa
            // recensione. Lo uso al posto di rileggere l'intera lista, che conterrebbe anche le
            // recensioni delle esecuzioni precedenti (il DB e' persistente) e falserebbe l'esito.
            String salvato = leggi(creazione.body()).path("testo").asText("");

            // Controprova aggiuntiva: la stessa recensione deve ripresentarsi identica in lettura.
            HttpResponse<String> lettura = get("/api/prodotti/" + prodottoId + "/recensioni", null);

            boolean salvatoGrezzo = salvato.equals(payload);
            boolean salvatoEscapato = salvato.equals(escapeAtteso(payload));

            Esito esito;
            String evidenza;
            if (salvatoGrezzo) {
                esito = Esito.VULNERABILE;
                evidenza = "Il server ha salvato il markup IDENTICO (testo=\"" + salvato
                        + "\"): verra' eseguito dal browser che apre la pagina.";
            } else if (salvatoEscapato) {
                esito = Esito.SICURO;
                evidenza = "Il server ha neutralizzato i caratteri (testo=\"" + salvato
                        + "\"): mostrato come testo, non eseguito.";
            } else {
                esito = Esito.DA_VERIFICARE;
                evidenza = "Testo salvato inatteso (\"" + salvato + "\"), ne' grezzo ne' escapato. " + estratto(lettura);
            }

            findings.add(new Finding("Agente XSS", "/api/prodotti/{id}/recensioni", "POST→GET",
                    "Stored XSS: scrivi markup, rileggi la risposta", payload, esito, evidenza));
        }

        ReportWriter.scrivi("Agente XSS", "report-xss", findings);
    }

    private long primoProdottoId() {
        JsonNode prodotti = leggi(get("/api/prodotti", null).body());
        if (!prodotti.isArray() || prodotti.isEmpty()) {
            throw new IllegalStateException("Nessun prodotto disponibile per il test XSS");
        }
        return prodotti.get(0).get("id").asLong();
    }

    /** Come apparirebbe il payload dopo un htmlEscape corretto lato server. */
    private String escapeAtteso(String payload) {
        return payload.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
