package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "utenti")
@Getter
@Setter
@NoArgsConstructor
public class Utente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Qui ci finisce SOLO l'hash BCrypt, mai la password in chiaro.
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Ruolo ruolo = Ruolo.USER;

    public Utente(String email, String password, String nome, Ruolo ruolo) {
        this.email = email;
        this.password = password;
        this.nome = nome;
        this.ruolo = ruolo;
    }

    // --- UserDetails: e' cosi' che Spring Security legge i ruoli per @PreAuthorize ---
    // Il prefisso "ROLE_" e' la convenzione che hasRole("ADMIN") si aspetta di trovare.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.ruolo.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
