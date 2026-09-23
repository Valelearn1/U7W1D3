package com.example.demo.auth;

import com.example.demo.config.VulnerabilityFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // attiva @PreAuthorize sui metodi dei controller
public class SecurityConfig {

    /**
     * L'origine da cui il frontend ha il permesso di chiamare questa API.
     * In locale http://localhost:5173, su Render l'indirizzo del sito statico.
     * E' UNA sola variabile, ALLOWED_ORIGIN: lo stesso nome in application.yml e in render.yaml.
     */
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    /**
     * In locale frontend e backend stanno sulla stessa origine, perche' il proxy di Vite
     * inoltra /api alla 8080: il browser non fa nemmeno partire il controllo CORS.
     * Su Render invece sono due domini diversi (radici-fe e radici-be), quindi ogni fetch
     * e' cross-origin: senza questa configurazione il browser blocca la risposta.
     *
     * Con Spring Security il CORS va acceso QUI, nella catena dei filtri: i filtri di sicurezza
     * girano prima di Spring MVC, quindi una @CrossOrigin sul controller arriverebbe troppo tardi
     * e la richiesta preflight (OPTIONS, senza token) verrebbe respinta prima di essere letta.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configurazione = new CorsConfiguration();
        configurazione.setAllowedOrigins(List.of(allowedOrigin));
        configurazione.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configurazione.setAllowedHeaders(List.of("*"));
        // Serve alla superficie legacy, l'unica che viaggia con un cookie.
        configurazione.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource sorgente = new UrlBasedCorsConfigurationSource();
        sorgente.registerCorsConfiguration("/**", configurazione);
        return sorgente;
    }

    /**
     * CATENA 1 - la superficie "legacy", quella che si autentica con un COOKIE.
     * E' l'unico posto dove il CSRF ha senso, perche' e' l'unico posto dove il browser
     * allega la credenziale da solo. Bersaglio dell'Agente CSRF.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain legacyFilterChain(HttpSecurity http, AuthFilter authFilter, VulnerabilityFlags flags)
            throws Exception {

        http.securityMatcher("/api/legacy/**");
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (flags.isCsrf()) {
            // ===== VERSIONE VULNERABILE =====
            // "tanto uso JWT, il CSRF non mi serve" -> ma qui il JWT sta in un COOKIE,
            // quindi il browser lo allega anche alle richieste partite da un sito ostile.
            // Risultato: una <form> su evil.com puo' eseguire scritture a nome dell'utente loggato.
            http.csrf(csrf -> csrf.disable());
        } else {
            // ===== VERSIONE SICURA =====
            // Schema "double submit cookie": il server mette il token in un cookie leggibile da JS
            // (XSRF-TOKEN) e pretende di riaverlo indietro nell'header X-XSRF-TOKEN.
            // Un sito ostile puo' far partire la richiesta col cookie, ma NON puo' leggere il
            // cookie di un altro dominio (lo impedisce la same-origin policy), quindi l'header
            // non riesce a compilarlo e la richiesta viene rifiutata con 403.
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    // Handler "semplice": il valore nel cookie e quello atteso nell'header coincidono.
                    // (Quello di default li offusca con uno XOR come difesa extra contro BREACH.)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        }

        http.authorizeHttpRequests(req -> req.requestMatchers("/**").permitAll());
        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CATENA 2 - l'API vera, stateless, token nell'header Authorization.
     * Qui csrf.disable() e' CORRETTO: senza credenziali ambientali il CSRF non e' realizzabile.
     */
    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http, AuthFilter authFilter) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(req -> req.requestMatchers("/**").permitAll());
        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * AuthFilter e' un @Component che estende OncePerRequestFilter: Spring Boot lo registrerebbe
     * anche nella catena di filtri del servlet container, cioe' FUORI da Spring Security.
     * Cosi' girerebbe due volte e anche su rotte dove non lo vogliamo. Qui disattivo
     * la registrazione automatica: deve girare solo dove l'ho agganciato io.
     */
    @Bean
    public FilterRegistrationBean<AuthFilter> disattivaRegistrazioneAutomatica(AuthFilter filter) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** BCrypt genera un salt casuale per ogni password e lo salva dentro l'hash stesso. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
