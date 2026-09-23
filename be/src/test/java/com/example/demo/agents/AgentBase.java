package com.example.demo.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Base comune ai quattro agenti.
 *
 * Nota la scelta del client HTTP: non uso gli helper di Spring ma java.net.http.HttpClient,
 * cioe' un client "stupido" che manda esattamente i byte che gli dico e mi restituisce
 * esattamente cio' che arriva, status code compresi. Un agente di sicurezza deve poter
 * mandare richieste malformate, omettere header, mentire sul Content-Type: qualsiasi
 * comodita' del framework qui sarebbe d'intralcio, perche' nasconderebbe il comportamento
 * reale dell'app dietro conversioni ed eccezioni automatiche.
 *
 * L'app viene avviata davvero su una porta casuale (webEnvironment = RANDOM_PORT):
 * gli agenti la attaccano dall'esterno via HTTP, come farebbe un client qualsiasi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AgentBase {

    @LocalServerPort
    protected int porta;

    protected static final ObjectMapper JSON = new ObjectMapper();

    protected final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER) // un redirect e' un'informazione, non un fastidio
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    protected String tokenAdmin;
    protected String tokenUser;
    protected String tokenAltro;

    @BeforeEach
    void autenticati() {
        this.tokenAdmin = login("admin@demo.it", "adminadmin");
        this.tokenUser = login("user@demo.it", "useruser12");
        this.tokenAltro = login("altro@demo.it", "useruser12");
    }

    protected String base() {
        return "http://localhost:" + porta;
    }

    // ---------------------------------------------------------------- HTTP

    protected HttpResponse<String> get(String path, String token) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base() + path)).GET();
        if (token != null) b.header("Authorization", "Bearer " + token);
        return invia(b.build());
    }

    protected HttpResponse<String> post(String path, Object body, String token) {
        return post(path, body, token, Map.of());
    }

    protected HttpResponse<String> post(String path, Object body, String token, Map<String, String> headerExtra) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serializza(body)));
        if (token != null) b.header("Authorization", "Bearer " + token);
        headerExtra.forEach(b::header);
        return invia(b.build());
    }

    protected HttpResponse<String> delete(String path, String token) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base() + path)).DELETE();
        if (token != null) b.header("Authorization", "Bearer " + token);
        return invia(b.build());
    }

    protected HttpResponse<String> invia(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Richiesta fallita verso " + request.uri(), e);
        }
    }

    // ---------------------------------------------------------------- utilita'

    protected String login(String email, String password) {
        HttpResponse<String> r = post("/api/auth/login", Map.of("email", email, "password", password), null);
        if (r.statusCode() != 200) {
            throw new IllegalStateException("Login fallito per " + email + ": " + r.statusCode() + " " + r.body());
        }
        return leggi(r.body()).get("accessToken").asText();
    }

    protected JsonNode leggi(String body) {
        try {
            return JSON.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Risposta non JSON: " + body, e);
        }
    }

    protected String serializza(Object o) {
        if (o instanceof String s) return s;
        try {
            return JSON.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Quanti elementi ha restituito un endpoint di lista. -1 se la risposta non e' un array. */
    protected int conta(HttpResponse<String> r) {
        try {
            JsonNode n = leggi(r.body());
            return n.isArray() ? n.size() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    protected String estratto(HttpResponse<String> r) {
        String b = r.body() == null ? "" : r.body().replaceAll("\\s+", " ").trim();
        return "HTTP " + r.statusCode() + " — " + (b.length() > 200 ? b.substring(0, 200) + "…" : b);
    }
}
