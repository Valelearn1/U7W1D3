// Una singola recensione.
//
// Il testo passa da { }, e React escapa tutto quello che ci finisce dentro: se una
// recensione contiene <img onerror=...> viene mostrata come testo, non eseguita.
// E' il comportamento predefinito, e qui non viene disattivato da nessuna parte.
//
// Nel progetto di ieri esisteva un interruttore che sostituiva questa riga con
// dangerouslySetInnerHTML, per far vedere da dove entra un XSS in un'app React.
// Sul sito pubblicato non c'e': restava una porta aperta senza motivo.
export default function Recensione({ recensione }) {
  return (
    <li className="recensione">
      <div className="recensione-head">
        <strong>{recensione.autore}</strong>
        <span className="voto">{'★'.repeat(recensione.voto)}</span>
      </div>
      <p className="recensione-testo">{recensione.testo}</p>
    </li>
  )
}
