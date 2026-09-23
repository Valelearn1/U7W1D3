package com.example.demo.services;

import com.example.demo.auth.JWTTools;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.entities.Utente;
import com.example.demo.exceptions.UnauthorizedException;
import com.example.demo.repositories.UtenteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTTools jwtTools;

    public AuthService(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder, JWTTools jwtTools) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTools = jwtTools;
    }

    public LoginResponseDTO login(LoginDTO body) {
        // Nota il messaggio d'errore UNICO per "email inesistente" e "password sbagliata":
        // distinguerli permetterebbe a un attaccante di scoprire quali email sono registrate.
        Utente utente = utenteRepository.findByEmail(body.email())
                .orElseThrow(() -> new UnauthorizedException("Credenziali non valide"));

        if (!passwordEncoder.matches(body.password(), utente.getPassword())) {
            throw new UnauthorizedException("Credenziali non valide");
        }

        return new LoginResponseDTO(jwtTools.generateToken(utente), utente.getEmail(), utente.getRuolo().name());
    }
}
