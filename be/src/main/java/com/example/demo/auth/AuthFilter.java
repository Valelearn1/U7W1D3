package com.example.demo.auth;

import com.example.demo.entities.Utente;
import com.example.demo.exceptions.UnauthorizedException;
import com.example.demo.services.UtenteService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Legge il token dalla richiesta, lo verifica e mette l'utente nel SecurityContext.
 * Da li' in poi @PreAuthorize e @AuthenticationPrincipal hanno su cosa lavorare.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired private JWTTools jwtTools;
    @Autowired private UtenteService utenteService;

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // LIVELLO 1 dell'autorizzazione: qui il filtro decide "loggato o no".
        // Un filtro gira PRIMA del DispatcherServlet, quindi @RestControllerAdvice non
        // vede le eccezioni lanciate qui: se ci limitassimo a lanciare, otterremmo un 500.
        // Percio' l'esito "non autenticato" lo scriviamo noi come 401, a mano.
        try {
            Optional<String> token = estraiToken(request);

            if (token.isEmpty()) {
                // Nessun token. Sulle rotte ad AUTENTICAZIONE OPZIONALE (la vetrina pubblica)
                // va bene: si prosegue come anonimi e il controller restituira' la vista pubblica.
                // Su tutte le altre invece il token e' obbligatorio -> 401.
                if (autenticazioneOpzionale(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                throw new UnauthorizedException("Token mancante: inserirlo nell'header Authorization");
            }

            // Token presente: deve essere valido (anche sulla vetrina). Se l'admin manda il suo
            // token, qui viene riconosciuto e il controller gli dara' la vista completa.
            jwtTools.verifyToken(token.get());

            long idUtente = jwtTools.extractIdFromToken(token.get());
            Utente utenteCorrente = utenteService.findById(idUtente);

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(utenteCorrente, null, utenteCorrente.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UnauthorizedException ex) {
            rispondi401(response, ex.getMessage());
            return; // NON proseguiamo la catena: la richiesta si ferma qui con 401
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Rotte ad autenticazione OPZIONALE: la vetrina pubblica (GET /api/prodotti/**).
     * Sono accessibili senza token, ma se un token c'e' viene comunque letto, cosi' l'admin
     * riceve la vista completa. E' questo che permette "tre risposte diverse allo stesso indirizzo".
     */
    private boolean autenticazioneOpzionale(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && MATCHER.match("/api/prodotti/**", request.getServletPath());
    }

    // Scrive una risposta 401 in JSON senza passare dal DispatcherServlet.
    private void rispondi401(HttpServletResponse response, String messaggio) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = "{\"messaggio\":\"" + messaggio.replace("\"", "'") + "\"}";
        response.getWriter().write(json);
    }

    /**
     * Da dove arriva il token. Questo metodo e' il punto in cui si gioca tutta la storia del CSRF.
     *
     * 1) Header "Authorization: Bearer ...": il browser NON lo aggiunge da solo. Un sito malevolo
     *    puo' far partire una richiesta verso la nostra API, ma quell'header non ci sara' mai.
     *    Per questo un'API che si autentica SOLO cosi' e' immune al CSRF per costruzione,
     *    e disabilitare la protezione CSRF su di essa e' corretto, non e' una svista.
     *
     * 2) Cookie "accessToken": il browser lo allega AUTOMATICAMENTE a ogni richiesta verso il
     *    nostro dominio, anche a quelle partite da un altro sito. E' una "credenziale ambientale",
     *    ed e' esattamente la condizione che rende possibile il CSRF. Qui lo accettiamo solo
     *    sotto /api/legacy/**, che e' la superficie che l'Agente CSRF va a bersagliare.
     */
    private Optional<String> estraiToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }

        if (MATCHER.match("/api/legacy/**", request.getServletPath())) {
            return leggiCookie(request, "accessToken");
        }

        return Optional.empty();
    }

    private Optional<String> leggiCookie(HttpServletRequest request, String nome) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> nome.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * Rotte del tutto pubbliche, che il filtro salta completamente: login/registrazione
     * (non possono richiedere un token, altrimenti non ci si registra mai), /error e
     * l'health check.
     * La vetrina NON e' qui: passa dal filtro perche' il token, se presente, va letto
     * comunque (autenticazione opzionale, vedi sopra).
     *
     * /actuator/** e' l'indirizzo che Render interroga per sapere se il servizio e' sano,
     * e lo interroga senza token. Se il filtro lo trattasse come una rotta protetta
     * risponderebbe 401, Render non vedrebbe mai il servizio partire e il deploy resterebbe
     * appeso finche' non scade.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return MATCHER.match("/api/auth/**", path)
                || MATCHER.match("/actuator/**", path)
                || MATCHER.match("/error", path);
    }
}
