// L'UNICO punto del frontend che conosce l'indirizzo del backend.
// Nessuna pagina scrive URL a mano: passano tutte da qui.
//
// In sviluppo BASE e' vuota, le chiamate partono relative (/api/...) e le inoltra il proxy
// di Vite alla 8080. Su Render il proxy NON esiste: frontend e backend sono due domini
// diversi, quindi serve l'indirizzo completo. Arriva da VITE_API_URL, che Vite sostituisce
// al momento della BUILD - cambiarla richiede un nuovo deploy, non basta riavviare.
const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

// Il token JWT vive in una variabile di modulo, NON in un cookie. E' una scelta di sicurezza
// precisa: il browser non allega da solo l'header Authorization, quindi un sito ostile non
// puo' far partire richieste a nome nostro. Il backend accetta il token da cookie solo sotto
// /api/legacy/**, che esiste apposta per avere un bersaglio CSRF da studiare.
let token = null

export function setToken(nuovo) {
  token = nuovo
}

async function richiesta(percorso, { method = 'GET', body, auth = false } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth && token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE}${percorso}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const testo = await res.text()
  const dati = testo ? JSON.parse(testo) : null
  if (!res.ok) {
    // Sugli errori di validazione il server manda un messaggio generico ("Payload non
    // valido") e il motivo vero in dettagli: "password: deve avere almeno 8 caratteri".
    // Senza questa riga l'utente leggerebbe solo il generico e non saprebbe cosa correggere.
    const dettagli = dati?.dettagli
    if (Array.isArray(dettagli) && dettagli.length > 0) {
      throw new Error(dettagli.join(' · '))
    }
    throw new Error(dati?.messaggio || `Errore ${res.status}`)
  }
  return dati
}

export const api = {
  // Mostrato in pagina per capire a colpo d'occhio contro quale backend si sta parlando.
  indirizzo: BASE || '(stessa origine, proxy di Vite)',

  login: (email, password) =>
    richiesta('/api/auth/login', { method: 'POST', body: { email, password } }),

  // Registrazione aperta a chiunque: il ruolo lo decide il server, che assegna sempre
  // USER. Non e' un campo che il client possa mandare, altrimenti bastarebbe aggiungere
  // "ruolo": "ADMIN" alla richiesta per diventare amministratori.
  registra: (nome, email, password) =>
    richiesta('/api/auth/register', { method: 'POST', body: { nome, email, password } }),

  // Il catalogo e' autenticato in modo OPZIONALE: se sono loggata mando il token (auth:true) e
  // il server puo' darmi la vista admin; se non lo sono, ricevo quella pubblica. Stesso indirizzo.
  prodotti: () => richiesta('/api/prodotti', { auth: true }),
  cerca: (q) => richiesta(`/api/prodotti/cerca?q=${encodeURIComponent(q)}`, { auth: true }),
  prodotto: (id) => richiesta(`/api/prodotti/${id}`, { auth: true }),
  pubblicaProdotto: (id) =>
    richiesta(`/api/prodotti/${id}/pubblica`, { method: 'PATCH', auth: true }),
  // Creazione: solo ADMIN (il backend la protegge con @PreAuthorize, livello 2).
  creaProdotto: (body) => richiesta('/api/prodotti', { method: 'POST', body, auth: true }),

  recensioni: (prodottoId) => richiesta(`/api/prodotti/${prodottoId}/recensioni`),
  aggiungiRecensione: (prodottoId, testo, voto) =>
    richiesta(`/api/prodotti/${prodottoId}/recensioni`, {
      method: 'POST',
      body: { testo, voto },
      auth: true,
    }),

  // --- Preferiti (LIVELLO 3: ogni chiamata e' ristretta al proprietario dal backend) ---
  // Il backend restituisce SOLO i preferiti dell'utente del token, mai quelli di un altro:
  // e' la query a deciderlo, non il frontend.
  mieiPreferiti: () => richiesta('/api/preferiti', { auth: true }),
  aggiungiPreferito: (prodottoId) =>
    richiesta(`/api/preferiti/${prodottoId}`, { method: 'POST', auth: true }),
  rimuoviPreferito: (preferitoId) =>
    richiesta(`/api/preferiti/${preferitoId}`, { method: 'DELETE', auth: true }),
}
