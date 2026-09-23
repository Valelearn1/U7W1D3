package com.example.demo.agents;

import com.example.demo.agents.report.EndpointInfo;
import com.example.demo.agents.report.MappaEndpoint;
import com.example.demo.agents.report.ReportWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AGENTE MAPPA
 *
 * Elenca endpoint, ingresso, uscita, protezione e ruoli in un file Markdown.
 * E' l'agente da far girare per primo: gli altri tre lavorano su cio' che questo scopre,
 * invece di avere le rotte scritte a mano dentro il codice del test (che invecchierebbero
 * al primo endpoint aggiunto).
 */
class MapAgentTest extends AgentBase {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("Agente mappa: genera SECURITY.md con la superficie d'attacco dell'API")
    void generaMappa() throws Exception {
        List<EndpointInfo> mappa = MappaEndpoint.genera(handlerMapping);

        assertFalse(mappa.isEmpty(), "Nessun endpoint trovato: l'applicazione non ha controller?");

        scriviMarkdown(mappa);
        scriviJson(mappa);

        System.out.println("\n>> [Agente mappa] " + mappa.size() + " endpoint mappati");

        // Un controllo che vale la pena automatizzare: ogni endpoint che SCRIVE deve avere
        // qualcosa fra se' e il mondo. Se ne spunta uno pubblico e in scrittura, quasi sempre
        // e' una svista, non una scelta.
        List<EndpointInfo> scrittureAperte = mappa.stream()
                .filter(EndpointInfo::scrittura)
                .filter(EndpointInfo::pubblico)
                // login e register SONO scritture pubbliche legittime: e' impossibile
                // autenticarsi senza un endpoint aperto. Le escludo dal controllo.
                .filter(e -> !e.path().startsWith("/api/auth/"))
                .toList();

        assertTrue(scrittureAperte.isEmpty(),
                "Endpoint di scrittura raggiungibili senza autenticazione: " + scrittureAperte.stream()
                        .map(e -> e.metodoHttp() + " " + e.path()).toList());
    }

    private void scriviMarkdown(List<EndpointInfo> mappa) throws Exception {
        StringBuilder md = new StringBuilder();
        md.append("# SECURITY.md — superficie d'attacco dell'API\n\n");
        md.append("_Generato automaticamente dall'**Agente mappa** il ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
          .append("._\n\n");
        md.append("Questo file non va modificato a mano: si rigenera con `./mvnw test -Dtest=MapAgentTest`.\n");
        md.append("La mappa e' letta a runtime dal registro di Spring MVC, quindi descrive ")
          .append("**cio' che l'applicazione sta davvero servendo**, non cio' che la documentazione crede.\n\n");
        md.append("Endpoint totali: **").append(mappa.size()).append("**");
        md.append(" — di cui in scrittura: **").append(mappa.stream().filter(EndpointInfo::scrittura).count()).append("**");
        md.append(", pubblici: **").append(mappa.stream().filter(EndpointInfo::pubblico).count()).append("**\n\n");

        md.append("| Metodo | Endpoint | Ingresso | Uscita | Protezione | Ruoli ammessi | CSRF |\n");
        md.append("|---|---|---|---|---|---|---|\n");
        for (EndpointInfo e : mappa) {
            md.append("| `").append(e.metodoHttp()).append("` ")
              .append("| `").append(e.path()).append("` ")
              .append("| ").append(e.ingresso().isEmpty() ? "—" : e.ingresso().stream().map(Object::toString).reduce((a, b) -> a + "<br>" + b).orElse("—")).append(" ")
              .append("| `").append(e.uscita()).append("` ")
              .append("| ").append(e.protezione()).append(" ")
              .append("| ").append(e.ruoli()).append(" ")
              .append("| ").append(e.csrf()).append(" |\n");
        }

        md.append("\n## Come leggere la colonna Protezione\n\n");
        md.append("- **PUBBLICO** — nessun token richiesto. Legittimo per login e lettura del catalogo, ");
        md.append("da guardare con sospetto su qualsiasi endpoint che scriva.\n");
        md.append("- **JWT via header** — il token va in `Authorization: Bearer`. Il browser non lo allega ");
        md.append("da solo, quindi queste rotte non sono attaccabili via CSRF.\n");
        md.append("- **JWT via cookie** — il browser lo allega automaticamente a ogni richiesta verso il ");
        md.append("nostro dominio, comprese quelle partite da un altro sito. Qui il CSRF serve davvero.\n");

        Path file = ReportWriter.cartella().resolve("SECURITY.md");
        Files.writeString(file, md.toString());
        System.out.println(">> [Agente mappa] scritto " + file.toAbsolutePath());
    }

    private void scriviJson(List<EndpointInfo> mappa) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(ReportWriter.cartella().resolve("endpoints.json").toFile(), mappa);
    }
}
