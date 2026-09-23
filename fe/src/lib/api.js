// In sviluppo BASE e' vuota e il proxy di Vite inoltra /api alla 8080.
// In produzione arriva da VITE_API_URL, iniettata durante la build.
const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

async function chiama(percorso, opzioni) {
  const risposta = await fetch(`${BASE}${percorso}`, {
    headers: { 'Content-Type': 'application/json' },
    ...opzioni,
  })
  if (!risposta.ok) {
    const testo = await risposta.text()
    throw new Error(testo || `${risposta.status} ${risposta.statusText}`)
  }
  return risposta.status === 204 ? undefined : risposta.json()
}

export const api = {
  indirizzo: BASE || '(stessa origine, proxy di Vite)',
  stato: () => chiama('/api/stato'),
}
