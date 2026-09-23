// Una singola recensione.
//
// ⚠️ QUI VIVE LA DIMOSTRAZIONE XSS ⚠️
//
// React, di default, ESCAPA tutto quello che metti in { }: se il testo contiene
// <img onerror=...> lo mostra come testo, non lo esegue. Ecco perche' un frontend
// React e' XSS-safe "gratis" e per creare la vulnerabilita' bisogna disattivare
// apposta questa difesa.
//
// La prop `modalitaVulnerabile` fa esattamente questo:
//   - false -> { recensione.testo }            React escapa, il markup e' innocuo
//   - true  -> dangerouslySetInnerHTML         il testo viene iniettato come HTML grezzo
//
// Il nome dell'API di React ("dangerously...") non e' un caso: e' l'unica porta da cui
// l'XSS puo' entrare in un'app React. Se il backend NON ha sanitizzato (flag app.vulnerable.xss=true)
// E il frontend usa questa porta, allora lo <script>/<img onerror> parte davvero.
export default function Recensione({ recensione, modalitaVulnerabile }) {
  return (
    <li className="recensione">
      <div className="recensione-head">
        <strong>{recensione.autore}</strong>
        <span className="voto">{'★'.repeat(recensione.voto)}</span>
      </div>

      {modalitaVulnerabile ? (
        // Porta aperta: il testo viene interpretato come HTML.
        <p
          className="recensione-testo"
          dangerouslySetInnerHTML={{ __html: recensione.testo }}
        />
      ) : (
        // Porta chiusa: React escapa, il markup resta testo.
        <p className="recensione-testo">{recensione.testo}</p>
      )}
    </li>
  )
}
