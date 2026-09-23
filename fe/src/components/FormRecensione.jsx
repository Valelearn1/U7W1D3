import { useState } from 'react'

// Form per aggiungere una recensione. Richiede di essere loggati (il token lo
// aggiunge lib/api.js).
export default function FormRecensione({ onInvia, disabilitato }) {
  const [testo, setTesto] = useState('')
  const [voto, setVoto] = useState(5)
  const [errore, setErrore] = useState(null)

  async function invia(e) {
    e.preventDefault()
    setErrore(null)
    try {
      await onInvia(testo, Number(voto))
      setTesto('')
    } catch (err) {
      setErrore(err.message)
    }
  }

  return (
    <form className="form-recensione" onSubmit={invia}>
      <label>
        La tua recensione
        <textarea
          value={testo}
          onChange={(e) => setTesto(e.target.value)}
          rows={2}
          placeholder="Com'è andata con questa pianta?"
          required
        />
      </label>
      <div className="form-riga">
        <label>
          Voto
          <select value={voto} onChange={(e) => setVoto(e.target.value)}>
            {[5, 4, 3, 2, 1].map((v) => (
              <option key={v} value={v}>
                {v} ★
              </option>
            ))}
          </select>
        </label>
        <button type="submit" disabled={disabilitato}>
          {disabilitato ? 'Fai il login per recensire' : 'Invia recensione'}
        </button>
      </div>
      {errore && <p className="errore">⚠️ {errore}</p>}
    </form>
  )
}
