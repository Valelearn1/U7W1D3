import { useEffect, useState } from 'react'
import { api, setToken } from './lib/api'
import Recensione from './components/Recensione'
import FormRecensione from './components/FormRecensione'
import './App.css'

// Emoji-copertina in base alla categoria della pianta.
const EMOJI = {
  'Da interno': '🪴',
  'Da esterno': '🌳',
  Grasse: '🌵',
  Fiorite: '🌸',
  Aromatiche: '🌿',
}
const emojiDi = (categoria) => EMOJI[categoria] || '🌱'

export default function App() {
  const [utente, setUtente] = useState(null)
  const [prodotti, setProdotti] = useState([])
  const [selezionato, setSelezionato] = useState(null)
  const [recensioni, setRecensioni] = useState([])
  const [preferiti, setPreferiti] = useState([])
  const [erroreGlobale, setErroreGlobale] = useState(null)

  const [ricerca, setRicerca] = useState('')
  const [risultati, setRisultati] = useState(null)
  const [categoria, setCategoria] = useState(null)
  const [mostraNuova, setMostraNuova] = useState(false)

  const isAdmin = utente?.ruolo === 'ADMIN'

  useEffect(() => {
    api.prodotti().then(setProdotti).catch((e) => setErroreGlobale(e.message))
  }, [])

  // Lista base (ricerca o catalogo intero), poi filtrata per categoria lato client.
  const base = risultati === null ? prodotti : risultati
  const daMostrare = categoria ? base.filter((p) => p.categoria === categoria) : base
  const categorie = [...new Set(prodotti.map((p) => p.categoria))]

  async function apriProdotto(p) {
    setSelezionato(p)
    setRecensioni(await api.recensioni(p.id))
  }

  function chiudiModale() {
    setSelezionato(null)
  }

  async function eseguiRicerca(e) {
    e.preventDefault()
    setCategoria(null)
    if (ricerca.trim() === '') {
      setRisultati(null)
      return
    }
    setRisultati(await api.cerca(ricerca))
  }

  function azzeraRicerca() {
    setRicerca('')
    setRisultati(null)
  }

  async function faiLogin(email, password) {
    const esito = await api.login(email, password)
    setToken(esito.accessToken)
    setUtente(esito)
    setPreferiti(await api.mieiPreferiti())
    // Ricarico il catalogo col token: il server ora mi da la vista del mio ruolo.
    setProdotti(await api.prodotti())
    setRisultati(null)
    setSelezionato(null)
  }

  async function logout() {
    setToken(null)
    setUtente(null)
    setPreferiti([])
    setSelezionato(null)
    setRisultati(null)
    setProdotti(await api.prodotti())
  }

  async function pubblica(prodotto) {
    await api.pubblicaProdotto(prodotto.id)
    const aggiornato = await api.prodotti()
    setProdotti(aggiornato)
    setSelezionato(aggiornato.find((p) => p.id === prodotto.id) || null)
  }

  async function creaPianta(body) {
    await api.creaProdotto(body)
    setProdotti(await api.prodotti())
    setMostraNuova(false)
  }

  function preferitoDi(prodottoId) {
    return preferiti.find((p) => p.prodottoId === prodottoId)
  }

  async function togglePreferito(prodotto) {
    const esistente = preferitoDi(prodotto.id)
    if (esistente) await api.rimuoviPreferito(esistente.id)
    else await api.aggiungiPreferito(prodotto.id)
    setPreferiti(await api.mieiPreferiti())
  }

  return (
    <div className="app">
      <header className="hero">
        <div className="hero-brand">
          <span className="logo">🌿</span>
          <div>
            <h1>Radici</h1>
            <p className="tagline">vivaio online — piante che crescono con te</p>
          </div>
        </div>
        <div className="hero-azioni">
          {isAdmin && (
            <button className="nuova" onClick={() => setMostraNuova(true)}>
              ➕ Aggiungi pianta
            </button>
          )}
          <BarraUtente utente={utente} onLogin={faiLogin} onLogout={logout} />
        </div>
      </header>

      {erroreGlobale && (
        <p className="errore banner">
          Vivaio non raggiungibile al momento. Riprova tra qualche istante.
        </p>
      )}

      <div className="toolbar">
        <form className="ricerca" onSubmit={eseguiRicerca}>
          <input
            type="text"
            value={ricerca}
            onChange={(e) => setRicerca(e.target.value)}
            placeholder="Cerca una pianta..."
          />
          <button type="submit">Cerca</button>
        </form>

        <div className="chips">
          <button
            className={`chip ${categoria === null ? 'attivo' : ''}`}
            onClick={() => setCategoria(null)}
          >
            Tutte
          </button>
          {categorie.map((c) => (
            <button
              key={c}
              className={`chip ${categoria === c ? 'attivo' : ''}`}
              onClick={() => setCategoria(c)}
            >
              {emojiDi(c)} {c}
            </button>
          ))}
        </div>
      </div>

      {risultati !== null && (
        <div className="ricerca-esito">
          <span>
            {daMostrare.length} risultat{daMostrare.length === 1 ? 'o' : 'i'} per “{ricerca}”
          </span>
          <button className="link" onClick={azzeraRicerca}>
            mostra tutto
          </button>
        </div>
      )}

      {utente && <IlMioGiardino preferiti={preferiti} onRimuovi={(id) => togglePreferito({ id })} />}

      <main className="griglia">
        {daMostrare.map((p) => (
          <Card
            key={p.id}
            prodotto={p}
            preferito={!!preferitoDi(p.id)}
            mostraStella={!!utente}
            onApri={() => apriProdotto(p)}
            onToggleFav={() => togglePreferito(p)}
          />
        ))}
        {daMostrare.length === 0 && <p className="vuoto">Nessuna pianta trovata.</p>}
      </main>

      {mostraNuova && (
        <ModaleNuovaPianta
          categorie={categorie}
          onCrea={creaPianta}
          onChiudi={() => setMostraNuova(false)}
        />
      )}

      {selezionato && (
        <ModaleProdotto
          prodotto={selezionato}
          recensioni={recensioni}
          utente={utente}
          isAdmin={isAdmin}
          preferito={!!preferitoDi(selezionato.id)}
          onToggleFav={() => togglePreferito(selezionato)}
          onPubblica={() => pubblica(selezionato)}
          onInviaRecensione={async (testo, voto) => {
            await api.aggiungiRecensione(selezionato.id, testo, voto)
            setRecensioni(await api.recensioni(selezionato.id))
          }}
          onChiudi={chiudiModale}
        />
      )}
    </div>
  )
}

function Card({ prodotto, preferito, mostraStella, onApri, onToggleFav }) {
  const bozza = prodotto.pubblicato === false
  return (
    <article className="card" onClick={onApri}>
      <div className="card-cover">
        <span className="cover-emoji">{emojiDi(prodotto.categoria)}</span>
        {bozza && <span className="badge-serra">IN SERRA</span>}
        {mostraStella && (
          <button
            className={`fav ${preferito ? 'attivo' : ''}`}
            title={preferito ? 'Nel mio giardino' : 'Aggiungi al giardino'}
            onClick={(e) => {
              e.stopPropagation()
              onToggleFav()
            }}
          >
            {preferito ? '★' : '☆'}
          </button>
        )}
      </div>
      <div className="card-body">
        <span className="card-cat">{prodotto.categoria}</span>
        <h3>{prodotto.nome}</h3>
        <p className="specie">{prodotto.descrizione}</p>
        <p className="card-prezzo">€ {prodotto.prezzo}</p>
      </div>
    </article>
  )
}

function IlMioGiardino({ preferiti, onRimuovi }) {
  return (
    <section className="giardino">
      <h2>🌱 Il mio giardino ({preferiti.length})</h2>
      <p className="giardino-nota">
        Solo le tue piante: la lista arriva dal server ristretta al proprietario. Cambia utente e
        vedrai un giardino diverso.
      </p>
      {preferiti.length === 0 ? (
        <p className="vuoto piccolo">Vuoto. Apri una pianta e premi la stella ☆.</p>
      ) : (
        <div className="giardino-chips">
          {preferiti.map((f) => (
            <span key={f.id} className="giardino-chip">
              {f.nomeProdotto}
              <button className="rimuovi" title="Togli dal giardino" onClick={() => onRimuovi(f.prodottoId)}>
                ×
              </button>
            </span>
          ))}
        </div>
      )}
    </section>
  )
}

function ModaleProdotto({
  prodotto, recensioni, utente, isAdmin, preferito,
  onToggleFav, onPubblica, onInviaRecensione, onChiudi,
}) {
  const bozza = prodotto.pubblicato === false
  return (
    <div className="modal-overlay" onClick={onChiudi}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="chiudi" onClick={onChiudi} title="Chiudi">
          ×
        </button>

        <div className="modal-head">
          <div className="modal-cover">
            <span>{emojiDi(prodotto.categoria)}</span>
          </div>
          <div>
            <span className="card-cat">{prodotto.categoria}</span>
            <h2>
              {prodotto.nome}
              {bozza && <span className="badge-serra grande">IN SERRA</span>}
            </h2>
            <p className="specie">{prodotto.descrizione}</p>
            <p className="modal-prezzo">€ {prodotto.prezzo}</p>
            {utente ? (
              <button className={`stella ${preferito ? 'attiva' : ''}`} onClick={onToggleFav}>
                {preferito ? '★ Nel mio giardino' : '☆ Aggiungi al giardino'}
              </button>
            ) : (
              <p className="vuoto piccolo">Entra per aggiungere al tuo giardino.</p>
            )}
          </div>
        </div>

        {isAdmin && prodotto.fornitore !== undefined && (
          <div className="pannello-admin">
            <h3>🔒 Dati riservati (solo ADMIN)</h3>
            <div className="griglia-admin">
              <div>
                <span className="etichetta">Fornitore</span>
                <strong>{prodotto.fornitore}</strong>
              </div>
              <div>
                <span className="etichetta">Prezzo d'acquisto</span>
                <strong>€ {prodotto.prezzoAcquisto}</strong>
              </div>
              <div>
                <span className="etichetta">Margine</span>
                <strong className="margine">€ {prodotto.margine}</strong>
              </div>
              <div>
                <span className="etichetta">Stato</span>
                <strong>{prodotto.pubblicato ? 'Pubblicata' : 'In serra'}</strong>
              </div>
            </div>
            {!prodotto.pubblicato && (
              <button className="pubblica" onClick={onPubblica}>
                📢 Pubblica questa pianta
              </button>
            )}
          </div>
        )}

        <h3>Recensioni ({recensioni.length})</h3>
        {recensioni.length === 0 ? (
          <p className="vuoto">Ancora nessuna recensione.</p>
        ) : (
          <ul className="lista-recensioni">
            {recensioni.map((r) => (
              <Recensione key={r.id} recensione={r} />
            ))}
          </ul>
        )}

        <FormRecensione onInvia={onInviaRecensione} disabilitato={!utente} />
      </div>
    </div>
  )
}

// Form di creazione pianta (solo ADMIN). Il backend rifiuta comunque la richiesta di
// chi non è admin: qui il bottone è nascosto, ma la vera difesa è @PreAuthorize sul server.
function ModaleNuovaPianta({ categorie, onCrea, onChiudi }) {
  const [form, setForm] = useState({
    nome: '',
    descrizione: '',
    prezzo: '',
    categoria: categorie[0] || 'Da interno',
    prezzoAcquisto: '',
    fornitore: '',
    pubblicato: true,
  })
  const [errore, setErrore] = useState(null)
  const set = (campo) => (e) =>
    setForm({ ...form, [campo]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  async function invia(e) {
    e.preventDefault()
    setErrore(null)
    try {
      await onCrea({
        nome: form.nome,
        descrizione: form.descrizione,
        prezzo: Number(form.prezzo),
        categoria: form.categoria,
        prezzoAcquisto: form.prezzoAcquisto === '' ? null : Number(form.prezzoAcquisto),
        fornitore: form.fornitore || null,
        pubblicato: form.pubblicato,
      })
    } catch (err) {
      setErrore(err.message)
    }
  }

  return (
    <div className="modal-overlay" onClick={onChiudi}>
      <div className="modal stretta" onClick={(e) => e.stopPropagation()}>
        <button className="chiudi" onClick={onChiudi} title="Chiudi">
          ×
        </button>
        <h2>➕ Aggiungi una pianta</h2>
        <form className="form-nuova" onSubmit={invia}>
          <label>
            Nome
            <input value={form.nome} onChange={set('nome')} required />
          </label>
          <label>
            Specie / nota di cura
            <input value={form.descrizione} onChange={set('descrizione')} />
          </label>
          <div className="form-due">
            <label>
              Prezzo di vendita (€)
              <input type="number" step="0.01" min="0" value={form.prezzo} onChange={set('prezzo')} required />
            </label>
            <label>
              Categoria
              <input list="categorie" value={form.categoria} onChange={set('categoria')} required />
              <datalist id="categorie">
                {categorie.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </label>
          </div>
          <div className="form-riservati">
            <p className="etichetta-sezione">🔒 Dati riservati (facoltativi)</p>
            <div className="form-due">
              <label>
                Prezzo d'acquisto (€)
                <input type="number" step="0.01" min="0" value={form.prezzoAcquisto} onChange={set('prezzoAcquisto')} />
              </label>
              <label>
                Fornitore
                <input value={form.fornitore} onChange={set('fornitore')} />
              </label>
            </div>
          </div>
          <label className="checkbox-riga">
            <input type="checkbox" checked={form.pubblicato} onChange={set('pubblicato')} />
            Pubblica subito (altrimenti nasce “in serra”, visibile solo agli admin)
          </label>
          {errore && <p className="errore">⚠️ {errore}</p>}
          <button type="submit">Crea pianta</button>
        </form>
      </div>
    </div>
  )
}

function BarraUtente({ utente, onLogin, onLogout }) {
  const [errore, setErrore] = useState(null)
  const [mostraFormAdmin, setMostraFormAdmin] = useState(false)
  const [passwordAdmin, setPasswordAdmin] = useState('')

  async function login(email, password) {
    setErrore(null)
    try {
      await onLogin(email, password)
    } catch (e) {
      setErrore(e.message)
    }
  }

  if (utente) {
    return (
      <div className="barra-utente">
        <span className="saluto">
          Ciao, <strong>{utente.email}</strong>
        </span>
        <span className="ruolo">{utente.ruolo}</span>
        <button className="logout" onClick={onLogout}>
          Esci
        </button>
      </div>
    )
  }

  return (
    <div className="barra-utente">
      <span className="anonimo">Anonimo</span>

      {/* L'utente normale e' un account dimostrativo: la sua password e' pubblica per
          costruzione, serve solo a far vedere il livello 3 (ognuno vede i propri preferiti).
          Tenerla qui non espone niente che non sia gia' scritto nel README. */}
      <button onClick={() => login('user@demo.it', 'useruser12')}>Entra come USER</button>

      {/* L'admin no: la sua password e' ADMIN_PASSWORD, scelta al momento del deploy e
          diversa su ogni installazione. Scriverla qui significherebbe pubblicarla, visto
          che il bundle del frontend e' scaricabile da chiunque apra il sito. Si digita. */}
      {mostraFormAdmin ? (
        <form
          className="login-admin"
          onSubmit={(e) => {
            e.preventDefault()
            login('admin@demo.it', passwordAdmin)
          }}
        >
          <input
            type="password"
            value={passwordAdmin}
            onChange={(e) => setPasswordAdmin(e.target.value)}
            placeholder="Password admin"
            autoFocus
          />
          <button type="submit">Entra</button>
          <button type="button" className="annulla" onClick={() => setMostraFormAdmin(false)}>
            Annulla
          </button>
        </form>
      ) : (
        <button onClick={() => setMostraFormAdmin(true)}>Entra come ADMIN</button>
      )}

      {errore && <span className="errore">⚠️ {errore}</span>}
    </div>
  )
}
