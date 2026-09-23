package com.example.demo.agents.report;

public enum Esito {
    VULNERABILE, // l'agente e' riuscito a fare quello che non doveva riuscire a fare
    SICURO,      // l'app si e' difesa come previsto
    DA_VERIFICARE // risposta ambigua: la segnalo ma non la conto come prova
}
