package com.example.demo.agents.report;

import java.util.List;

/** Una riga della mappa: tutto quello che si sa di un endpoint senza averlo ancora chiamato. */
public record EndpointInfo(
        String path,
        String metodoHttp,
        String handler,       // Classe#metodo che lo serve
        List<ParamInfo> ingresso,
        String uscita,        // tipo di ritorno
        String protezione,    // come si autentica chi lo chiama
        String ruoli,         // ruoli ammessi (da @PreAuthorize)
        String csrf           // stato della protezione CSRF sulla sua catena
) {
    /** Un parametro d'ingresso: da dove arriva e se qualcuno lo controlla. */
    public record ParamInfo(String nome, String tipo, String origine, boolean validato) {
        @Override
        public String toString() {
            return origine + " " + nome + ": " + tipo + (validato ? " ✅@Valid" : "");
        }
    }

    public boolean scrittura() {
        return List.of("POST", "PUT", "PATCH", "DELETE").contains(metodoHttp);
    }

    public boolean pubblico() {
        return protezione.startsWith("PUBBLICO");
    }
}
