package com.example.demo.repositories;

import com.example.demo.entities.Recensione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecensioneRepository extends JpaRepository<Recensione, Long> {
    List<Recensione> findByProdottoId(Long prodottoId);
}
