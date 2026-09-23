package com.example.demo.agents.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Scrive i report degli agenti su disco: un .md da leggere e un .json da dare in pasto ad altri tool. */
public class ReportWriter {

    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static Path cartella() {
        Path dir = Path.of(System.getProperty("app.audit.output-dir", "../reports"));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare la cartella dei report: " + dir, e);
        }
        return dir;
    }

    public static void scrivi(String nomeAgente, String nomeFile, List<Finding> findings) {
        long vulnerabili = findings.stream().filter(Finding::vulnerabile).count();

        StringBuilder md = new StringBuilder();
        md.append("# Report — ").append(nomeAgente).append("\n\n");
        md.append("_Generato il ").append(LocalDateTime.now().format(ORA)).append("_\n\n");
        md.append(vulnerabili > 0
                ? "## 🔴 " + vulnerabili + " problema/i trovato/i su " + findings.size() + " prove\n\n"
                : "## 🟢 Nessun problema trovato su " + findings.size() + " prove\n\n");

        md.append("| Esito | Endpoint | Prova | Payload | Evidenza |\n");
        md.append("|---|---|---|---|---|\n");
        for (Finding f : findings) {
            md.append("| ").append(simbolo(f.esito()))
              .append(" | `").append(f.metodoHttp()).append(" ").append(f.endpoint()).append("`")
              .append(" | ").append(f.prova())
              .append(" | `").append(escapePipe(abbrevia(f.payload(), 60))).append("`")
              .append(" | ").append(escapePipe(abbrevia(f.evidenza(), 120)))
              .append(" |\n");
        }

        try {
            Files.writeString(cartella().resolve(nomeFile + ".md"), md.toString());

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(cartella().resolve(nomeFile + ".json").toFile(),
                    Map.of("agente", nomeAgente,
                           "generatoIl", LocalDateTime.now().toString(),
                           "proveTotali", findings.size(),
                           "vulnerabilitaTrovate", vulnerabili,
                           "findings", findings));
        } catch (IOException e) {
            throw new RuntimeException("Impossibile scrivere il report " + nomeFile, e);
        }

        System.out.println("\n>> [" + nomeAgente + "] " + findings.size() + " prove, "
                + vulnerabili + " vulnerabilita' — report in " + cartella().resolve(nomeFile + ".md").toAbsolutePath());
    }

    private static String simbolo(Esito e) {
        return switch (e) {
            case VULNERABILE -> "🔴 VULNERABILE";
            case SICURO -> "🟢 sicuro";
            case DA_VERIFICARE -> "🟡 da verificare";
        };
    }

    private static String abbrevia(String s, int max) {
        if (s == null) return "";
        String pulito = s.replaceAll("\\s+", " ").trim();
        return pulito.length() <= max ? pulito : pulito.substring(0, max) + "…";
    }

    private static String escapePipe(String s) {
        return s.replace("|", "\\|");
    }
}
