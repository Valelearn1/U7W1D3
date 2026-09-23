package com.example.demo.dto;

// Nota: nessun campo password. Il DTO di uscita espone solo cio' che e' lecito far vedere.
public record UtenteDTO(Long id, String nome, String email, String ruolo) {}
