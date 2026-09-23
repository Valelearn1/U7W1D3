package com.example.demo.controllers;

import com.example.demo.dto.*;
import com.example.demo.entities.Ruolo;
import com.example.demo.services.AuthService;
import com.example.demo.services.UtenteService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UtenteService utenteService;

    public AuthController(AuthService authService, UtenteService utenteService) {
        this.authService = authService;
        this.utenteService = utenteService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UtenteDTO register(@RequestBody @Valid RegistrazioneDTO body) {
        return utenteService.registra(body, Ruolo.USER);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody @Valid LoginDTO body, HttpServletResponse response) {
        LoginResponseDTO esito = authService.login(body);

        // Oltre a restituire il token nel JSON (uso normale, header Authorization),
        // lo mettiamo anche in un cookie: serve alla superficie /api/legacy/**, che e'
        // il bersaglio dell'Agente CSRF. Vedi AuthFilter per il perche'.
        Cookie cookie = new Cookie("accessToken", esito.accessToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return esito;
    }
}
