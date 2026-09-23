package com.example.demo.agents.report;

/**
 * Una singola prova eseguita da un agente contro un endpoint.
 *
 * Il campo che conta davvero e' "evidenza": un report di sicurezza senza la prova concreta
 * di cosa e' successo non e' verificabile e non serve a chi deve poi correggere.
 */
public record Finding(
        String agente,
        String endpoint,
        String metodoHttp,
        String prova,       // cosa e' stato tentato
        String payload,     // con quale input
        Esito esito,
        String evidenza     // cosa ha risposto l'app, in concreto
) {
    public boolean vulnerabile() {
        return esito == Esito.VULNERABILE;
    }
}
