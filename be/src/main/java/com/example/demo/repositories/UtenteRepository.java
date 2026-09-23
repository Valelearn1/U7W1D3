package com.example.demo.repositories;

import com.example.demo.entities.Utente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtenteRepository extends JpaRepository<Utente, Long> {
    Optional<Utente> findByEmail(String email);
    boolean existsByEmail(String email);
}
