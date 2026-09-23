package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecensioneDTO(
        @NotBlank(message = "Il testo e' obbligatorio") String testo,
        @NotNull @Min(1) @Max(5) Integer voto
) {}
