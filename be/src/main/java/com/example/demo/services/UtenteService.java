package com.example.demo.services;

import com.example.demo.dto.RegistrazioneDTO;
import com.example.demo.dto.UtenteDTO;
import com.example.demo.entities.Ruolo;
import com.example.demo.entities.Utente;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repositories.UtenteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public UtenteService(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Utente findById(Long id) {
        return utenteRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    public Utente findByEmail(String email) {
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Utente con email " + email + " non trovato"));
    }

    public UtenteDTO registra(RegistrazioneDTO body, Ruolo ruolo) {
        if (utenteRepository.existsByEmail(body.email())) {
            throw new BadRequestException("Email gia' registrata");
        }
        Utente nuovo = new Utente(
                body.email(),
                passwordEncoder.encode(body.password()), // mai salvare la password in chiaro
                body.nome(),
                ruolo
        );
        return toDTO(utenteRepository.save(nuovo));
    }

    public List<UtenteDTO> findAll() {
        return utenteRepository.findAll().stream().map(this::toDTO).toList();
    }

    public UtenteDTO toDTO(Utente u) {
        return new UtenteDTO(u.getId(), u.getNome(), u.getEmail(), u.getRuolo().name());
    }
}
