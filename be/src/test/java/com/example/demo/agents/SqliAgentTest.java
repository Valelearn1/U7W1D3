package com.example.demo.agents;

import com.example.demo.agents.report.Esito;
import com.example.demo.agents.report.Finding;
import com.example.demo.agents.report.ReportWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * AGENTE SQLi
 *
 * Prova l'iniezione SQL sull'endpoint di ricerca e riporta quello che passa.
 *
 * Come fa a "sapere" se e' passata, senza vedere il database? Guarda il COMPORTAMENTO:
 * una ricerca onesta con una stringa che non esiste deve tornare pochi o zero risultati.
 * Se iniettando "OR '1'='1" ne tornano molti di piu' (l'intero catalogo), vuol dire che
 * la condizione che ho scritto io e' finita dentro la query e ne ha cambiato il senso.
 * E' lo scarto fra i due numeri la prova, non il numero in se'.
 */
class SqliAgentTest extends AgentBase {

    // Payload noti e innocui: servono solo a RILEVARE il comportamento, non a fare danni.
    // Niente DROP, niente DELETE: un auditor dimostra il buco, non lo sfrutta per distruggere.
    private static final List<String> PAYLOAD_SEMPRE_VERO = List.of(
            "%' OR '1'='1",
            "zzz' OR 1=1 --",
            "' OR ''='"
    );

    @Test
    @DisplayName("Agente SQLi: tenta injection sulla ricerca prodotti")
    void testaRicerca() {
        List<Finding> findings = new ArrayList<>();

        // Passo 1 — baseline. Cerco una stringa che di sicuro non corrisponde a niente.
        // In un'app sana questo torna 0 risultati; e' il mio metro di paragone.
        String assurdo = "prodotto-inesistente-xyz123";
        int risultatiBaseline = conta(get("/api/prodotti/cerca?q=" + url(assurdo), null));
        int totaleCatalogo = conta(get("/api/prodotti", null));

        System.out.println(">> [Agente SQLi] baseline: '" + assurdo + "' -> " + risultatiBaseline
                + " risultati (catalogo intero: " + totaleCatalogo + ")");

        // Passo 2 — attacco. Se l'injection funziona, la condizione diventa sempre vera
        // e mi ritrovo l'intero catalogo pur avendo cercato una stringa impossibile.
        for (String payload : PAYLOAD_SEMPRE_VERO) {
            HttpResponse<String> r = get("/api/prodotti/cerca?q=" + url(payload), null);
            int risultati = conta(r);

            Esito esito;
            String evidenza;
            if (r.statusCode() >= 500) {
                // L'app e' esplosa sul mio apice: l'input tocca il motore SQL (segnale di fragilita'),
                // ma non ho ancora la prova di aver estratto dati. Lo segnalo come da verificare.
                esito = Esito.DA_VERIFICARE;
                evidenza = "L'app risponde 500 sul payload: l'input raggiunge il motore SQL. " + estratto(r);
            } else if (risultati > risultatiBaseline && risultati >= totaleCatalogo) {
                esito = Esito.VULNERABILE;
                evidenza = "La ricerca di una stringa impossibile ha restituito " + risultati
                        + " risultati (baseline " + risultatiBaseline + ", catalogo " + totaleCatalogo
                        + "): la condizione iniettata ha bypassato il filtro.";
            } else {
                esito = Esito.SICURO;
                evidenza = "Il payload e' stato trattato come testo di ricerca: " + risultati
                        + " risultati, come una query onesta. " + estratto(r);
            }

            findings.add(new Finding("Agente SQLi", "/api/prodotti/cerca", "GET",
                    "Injection con condizione sempre vera", payload, esito, evidenza));
        }

        // Passo 3 — UNION-based: il tentativo classico di leggere un'ALTRA tabella (gli utenti).
        String union = "zzz' UNION SELECT email, password, nome, ruolo, id FROM utenti --";
        HttpResponse<String> rUnion = get("/api/prodotti/cerca?q=" + url(union), null);
        boolean trapelaEmail = rUnion.body() != null && rUnion.body().contains("@demo.it");
        findings.add(new Finding("Agente SQLi", "/api/prodotti/cerca", "GET",
                "UNION SELECT per leggere la tabella utenti", union,
                trapelaEmail ? Esito.VULNERABILE : (rUnion.statusCode() >= 500 ? Esito.DA_VERIFICARE : Esito.SICURO),
                trapelaEmail
                        ? "Nella risposta compaiono email dalla tabella utenti: dati esfiltrati via UNION."
                        : "Nessun dato utente nella risposta. " + estratto(rUnion)));

        ReportWriter.scrivi("Agente SQLi", "report-sqli", findings);
    }

    private String url(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
