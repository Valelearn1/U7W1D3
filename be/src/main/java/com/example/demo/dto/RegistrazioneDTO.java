package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrazioneDTO(
        @NotBlank(message = "Il nome e' obbligatorio") String nome,
        @NotBlank @Email(message = "Email non valida") String email,
        @NotBlank @Size(min = 8, message = "La password deve avere almeno 8 caratteri") String password
) {}
