package com.example.demo.agents;

import com.example.demo.agents.report.Esito;
import com.example.demo.agents.report.Finding;
import com.example.demo.agents.report.ReportWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * AGENTE CSRF
 *
 * Invia una scrittura senza token e verifica che venga rifiutata.
 *
 * Il CSRF ha senso solo dove la credenziale e' AMBIENTALE, cioe' allegata dal browser da sola.
 * Nella nostra app e' il cookie accessToken sotto /api/legacy/**. L'agente simula quello che
 * farebbe una pagina ostile: manda la richiesta con SOLO il cookie e SENZA il token anti-CSRF.
 *
 *   - se la richiesta passa  -> l'app accetta scritture "a nome della vittima": VULNERABILE
 *   - se torna 403           -> la protezione ha fatto il suo lavoro: SICURO
 *
 * Come controprova, l'agente rifa' la stessa richiesta CON il token: deve funzionare, altrimenti
 * la protezione sarebbe solo un blocco cieco e non una vera difesa selettiva.
 */
class CsrfAgentTest extends AgentBase {

    @Test
    @DisplayName("Agente CSRF: scrittura via cookie senza token anti-CSRF")
    void testaScritturaLegacy() {
        List<Finding> findings = new ArrayList<>();

        // Mi procuro il cookie accedendo come admin (l'endpoint sensibile richiede ruolo ADMIN):
        // il login deposita accessToken. Questa e' la credenziale ambientale che il browser
        // di una vittima loggata allegherebbe da solo.
        String cookie = "accessToken=" + tokenAdmin;
        long prodottoId = 1L;
        String corpo = "{\"prezzo\": 0.01}"; // svaluto il prodotto: scrittura chiaramente illecita

        // --- Attacco: cookie presente, NESSUN token anti-CSRF (come farebbe evil.com) ---
        HttpResponse<String> attacco = inviaConCookie(
                "/api/legacy/prodotti/" + prodottoId + "/prezzo", corpo, cookie, null, null);

        Esito esitoAttacco;
        String evidenza;
        if (attacco.statusCode() == 403) {
            esitoAttacco = Esito.SICURO;
            evidenza = "Scrittura rifiutata con 403: senza token anti-CSRF non passa. " + estratto(attacco);
        } else if (attacco.statusCode() >= 200 && attacco.statusCode() < 300) {
            esitoAttacco = Esito.VULNERABILE;
            evidenza = "Scrittura ANDATA A BUON FINE col solo cookie e senza token: "
                    + "una pagina ostile potrebbe eseguirla a nome di un admin loggato. " + estratto(attacco);
        } else {
            esitoAttacco = Esito.DA_VERIFICARE;
            evidenza = "Risposta inattesa (" + attacco.statusCode() + "), non e' un chiaro accetta/rifiuta. " + estratto(attacco);
        }

        findings.add(new Finding("Agente CSRF", "/api/legacy/prodotti/{id}/prezzo", "POST",
                "Scrittura con credenziale via cookie e senza token anti-CSRF", "cookie accessToken, no CSRF token",
                esitoAttacco, evidenza));

        // --- Controprova: stessa richiesta ma FATTA BENE (token letto dall'endpoint apposito) ---
        HttpResponse<String> prep = inviaConCookie("/api/legacy/csrf", null, cookie, null, "GET");
        String token = leggi(prep.body()).path("token").asText("");
        String cookieCsrf = estraiCookieDaRisposta(prep, "XSRF-TOKEN");

        if (!token.isBlank()) {
            String cookieCompleto = cookie + (cookieCsrf != null ? "; XSRF-TOKEN=" + cookieCsrf : "");
            HttpResponse<String> legittima = inviaConCookie(
                    "/api/legacy/prodotti/" + prodottoId + "/prezzo", corpo, cookieCompleto, token, null);

            boolean ok = legittima.statusCode() >= 200 && legittima.statusCode() < 300;
            findings.add(new Finding("Agente CSRF", "/api/legacy/prodotti/{id}/prezzo", "POST",
                    "Controprova: stessa scrittura CON token anti-CSRF valido", "cookie + header X-XSRF-TOKEN",
                    ok ? Esito.SICURO : Esito.DA_VERIFICARE,
                    ok ? "Con il token corretto la scrittura passa: la difesa e' selettiva, non un blocco cieco. " + estratto(legittima)
                       : "Con il token la richiesta NON passa: la protezione blocca anche le richieste legittime. " + estratto(legittima)));
        } else {
            findings.add(new Finding("Agente CSRF", "/api/legacy/csrf", "GET",
                    "Controprova: recupero del token anti-CSRF", "—", Esito.DA_VERIFICARE,
                    "L'endpoint non ha fornito un token (protezione disattivata?): controprova non eseguibile. " + estratto(prep)));
        }

        ReportWriter.scrivi("Agente CSRF", "report-csrf", findings);
    }

    /** POST (o GET) verso l'app costruito a mano, per controllare cookie e header a piacere. */
    private HttpResponse<String> inviaConCookie(String path, String corpo, String cookie, String csrfToken, String metodo) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base() + path))
                .header("Cookie", cookie);

        if ("GET".equals(metodo)) {
            b.GET();
        } else {
            b.header("Content-Type", "application/json")
             .POST(HttpRequest.BodyPublishers.ofString(corpo == null ? "" : corpo));
        }
        if (csrfToken != null) b.header("X-XSRF-TOKEN", csrfToken);

        return invia(b.build());
    }

    private String estraiCookieDaRisposta(HttpResponse<String> r, String nome) {
        return r.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith(nome + "="))
                .map(c -> c.substring((nome + "=").length()).split(";", 2)[0])
                .findFirst()
                .orElse(null);
    }
}
