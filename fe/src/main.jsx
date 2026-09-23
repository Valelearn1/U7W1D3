import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// L'ordine di queste due righe conta: gli import ES si eseguono dall'alto in basso, quindi
// index.css (le variabili di base) va caricato PRIMA di App.jsx, che a sua volta importa
// App.css. Invertendole, il tema di Radici finirebbe nel foglio di stile prima di quello
// che deve sovrascrivere, e i colori tornerebbero quelli del template.
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
