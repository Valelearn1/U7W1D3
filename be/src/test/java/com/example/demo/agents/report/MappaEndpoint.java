package com.example.demo.agents.report;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Costruisce la mappa degli endpoint interrogando il registro di Spring.
 *
 * Il punto: NON facciamo parsing dei file sorgente. RequestMappingHandlerMapping e' il bean
 * che Spring MVC usa a runtime per decidere quale metodo Java serve quale richiesta HTTP:
 * contiene gia', in forma strutturata, path + verbo + metodo Java di ogni endpoint.
 * Da li' la reflection ci da' parametri, annotazioni e tipo di ritorno.
 *
 * Il vantaggio rispetto al leggere i sorgenti e' che questa mappa non puo' mentire:
 * e' letteralmente cio' che l'applicazione sta servendo in questo momento. Se un endpoint
 * esiste ma nessuno si ricorda di averlo scritto, qui compare lo stesso.
 */
public class MappaEndpoint {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    public static List<EndpointInfo> genera(RequestMappingHandlerMapping mapping) {
        List<EndpointInfo> mappa = new ArrayList<>();

        mapping.getHandlerMethods().forEach((info, handler) -> {
            for (String path : estraiPath(info)) {
                // Ignoro gli endpoint tecnici di Spring (/error, actuator...): non sono nostri.
                if (path.startsWith("/error")) continue;

                for (String verbo : estraiVerbi(info)) {
                    mappa.add(new EndpointInfo(
                            path,
                            verbo,
                            handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName(),
                            estraiParametri(handler),
                            handler.getMethod().getReturnType().getSimpleName(),
                            calcolaProtezione(path, verbo),
                            estraiRuoli(handler),
                            calcolaCsrf(path, verbo)
                    ));
                }
            }
        });

        mappa.sort(Comparator.comparing(EndpointInfo::path).thenComparing(EndpointInfo::metodoHttp));
        return mappa;
    }

    private static Set<String> estraiPath(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        return info.getDirectPaths();
    }

    private static List<String> estraiVerbi(RequestMappingInfo info) {
        var metodi = info.getMethodsCondition().getMethods();
        // Nessun verbo dichiarato = l'endpoint risponde a tutti. E' gia' di per se' una segnalazione.
        if (metodi.isEmpty()) return List.of("QUALSIASI");
        return metodi.stream().map(Enum::name).sorted().toList();
    }

    private static List<EndpointInfo.ParamInfo> estraiParametri(HandlerMethod handler) {
        List<EndpointInfo.ParamInfo> parametri = new ArrayList<>();

        for (var p : handler.getMethodParameters()) {
            String origine = origineDelParametro(p.getParameterAnnotations());
            if (origine == null) continue; // parametri iniettati da Spring, non input dell'utente

            boolean validato = false;
            for (Annotation a : p.getParameterAnnotations()) {
                if (a.annotationType().getSimpleName().equals("Valid")
                        || a.annotationType().getSimpleName().equals("Validated")) {
                    validato = true;
                }
            }

            parametri.add(new EndpointInfo.ParamInfo(
                    p.getParameterName() == null ? "?" : p.getParameterName(),
                    p.getParameterType().getSimpleName(),
                    origine,
                    validato));
        }
        return parametri;
    }

    private static String origineDelParametro(Annotation[] annotazioni) {
        for (Annotation a : annotazioni) {
            switch (a.annotationType().getSimpleName()) {
                case "RequestParam": return "query";
                case "PathVariable": return "path";
                case "RequestBody": return "body";
                case "RequestHeader": return "header";
                case "CookieValue": return "cookie";
            }
        }
        return null;
    }

    private static String estraiRuoli(HandlerMethod handler) {
        Method metodo = handler.getMethod();
        PreAuthorize sulMetodo = metodo.getAnnotation(PreAuthorize.class);
        if (sulMetodo != null) return "`" + sulMetodo.value() + "`";

        PreAuthorize sullaClasse = handler.getBeanType().getAnnotation(PreAuthorize.class);
        if (sullaClasse != null) return "`" + sullaClasse.value() + "` (sulla classe)";

        return "qualsiasi autenticato";
    }

    /**
     * Ricostruisce quale autenticazione serve per chiamare l'endpoint.
     * Queste regole rispecchiano AuthFilter.shouldNotFilter e AuthFilter.estraiToken:
     * se si tocca il filtro, va aggiornato anche qui — ed e' esattamente il tipo di
     * disallineamento che un progetto piu' maturo eviterebbe leggendo direttamente
     * la configurazione della SecurityFilterChain.
     */
    private static String calcolaProtezione(String path, String verbo) {
        if (MATCHER.match("/api/auth/**", path)) return "PUBBLICO (rotta di autenticazione)";
        if (MATCHER.match("/api/prodotti/**", path) && "GET".equals(verbo)) return "PUBBLICO (lettura libera)";
        if (MATCHER.match("/api/legacy/**", path)) return "JWT via **cookie** `accessToken`";
        return "JWT via header `Authorization: Bearer`";
    }

    private static String calcolaCsrf(String path, String verbo) {
        if (!List.of("POST", "PUT", "PATCH", "DELETE", "QUALSIASI").contains(verbo)) return "—";
        if (MATCHER.match("/api/legacy/**", path)) return "dipende da `app.vulnerable.csrf`";
        return "non applicabile (nessuna credenziale ambientale)";
    }
}
