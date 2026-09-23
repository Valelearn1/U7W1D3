# 🌿 Radici — vivaio online, pubblicato su Render

La vetrina di U7W1D2 trasferita nella base pronta per il deploy. Stesso sito, stessi tre
livelli di accesso, ma raggiungibile da chiunque a un indirizzo `onrender.com`.

| Parte | Tecnologia | In locale | Su Render |
|---|---|---|---|
| Backend | Spring Boot 4.1.1, Java 25, Maven wrapper | `be` sulla 8080 | Web Service (Docker) |
| Frontend | React 19, Vite, JavaScript (JSX) | `fe` sulla 5173 | Static Site |
| Database | PostgreSQL | locale sulla 5432 | Render PostgreSQL |

## I tre livelli di accesso

Lo stesso indirizzo risponde in modo diverso secondo chi lo chiede: è il **server** a decidere
cosa entra nel JSON, non la pagina.

| Chi | `GET /api/prodotti` | Cosa può fare |
|---|---|---|
| Anonimo | 16 piante, 5 campi | solo guardare il pubblicato |
| Utente | 16 piante, 5 campi | gestisce i **propri** preferiti (query ristretta al proprietario) |
| Admin | 19 piante, 9 campi | vede le bozze, `prezzoAcquisto`, `fornitore`, `margine`; pubblica |

Le tre difese stanno su tre piani diversi: `AuthFilter` (sei collegato?), `@PreAuthorize`
(il tuo ruolo te lo permette?), la query (questo dato è tuo?).

## Cos'è della base e cosa viene da ieri

Della base restano i pezzi che rendono il progetto pubblicabile — **non si toccano**:

```
render.yaml                                    blueprint: database + backend + frontend
be/Dockerfile                                  immagine Java 25, usata solo da Render
be/src/.../config/DatabaseUrl.java             DATABASE_URL -> formato JDBC
be/src/main/resources/application.yml          ${VARIABILE:ripiego}, nessun segreto
fe/src/lib/api.js                              l'unico punto che conosce l'indirizzo del BE
```

Di U7W1D2 sono arrivati **dominio** (`entities`, `repositories`, `services`, `dto`,
`exceptions`), **sicurezza** (`auth/`, `controllers/`) e **pagine** (`App.jsx`,
`components/`, `App.css`).

Tre punti dove i due pezzi si incontrano:

- `DemoApplication.main` chiama `DatabaseUrl.applica()` come **prima riga**: Spring legge la
  configurazione appena parte, quindi una traduzione più tardi arriverebbe a cose fatte.
- Il **CORS** si attiva dentro `SecurityConfig`, nella catena dei filtri, con una sola
  variabile `ALLOWED_ORIGIN`. Una `@CrossOrigin` sul controller arriverebbe troppo tardi:
  i filtri di sicurezza girano prima di Spring MVC e respingerebbero il preflight.
- `/actuator/**` è nella lista `shouldNotFilter` di `AuthFilter`. Render lo interroga senza
  token: se rispondesse 401 il deploy non finirebbe mai.

## Avvio in locale

1. PostgreSQL sulla 5432 con il database:
   ```
   createdb -U postgres U7W1D2
   ```
   Credenziali diverse da `postgres` / `postgres`: variabili `DB_URL`, `DB_USERNAME`,
   `DB_PASSWORD` (i ripieghi sono in `be/src/main/resources/application.yml`).
2. `./avvia.sh` (macOS/Linux) o doppio clic su `avvia.cmd` (Windows), oppure:
   ```
   cd be && ./mvnw spring-boot:run
   cd fe && npm install && npm run dev
   ```
3. http://localhost:5173 — in sviluppo il proxy di Vite inoltra `/api` alla 8080,
   quindi `VITE_API_URL` resta vuota.

### Accesso

Chi visita il sito si registra da sé con **Registrati** (nome, email, password da almeno
8 caratteri). Il ruolo lo assegna il server, sempre `USER`: non è un campo che il client
possa mandare, altrimenti basterebbe aggiungere `"ruolo": "ADMIN"` alla richiesta.

L'amministratore entra dallo **stesso** form, con `admin@demo.it` e il valore di
`ADMIN_PASSWORD`. Quella password non sta nel codice: il bundle del frontend è scaricabile
da chiunque apra il sito. Il seeder la riallinea a ogni avvio, quindi per cambiarla basta
modificare la variabile e rilanciare il servizio.

Il seeder crea anche `user@demo.it` e `altro@demo.it` (password `useruser12`), utili in
locale per vedere che due utenti diversi hanno giardini diversi.

## Deploy su Render

**1. I nomi, prima di tutto.** In `render.yaml` i tre nomi diventano i domini pubblici.
Se `radici-vivaio-*` risulta già preso, cambiarli **prima** del primo deploy: cambiarli dopo
significa cambiare indirizzo, e quindi rifare `ALLOWED_ORIGIN` e `VITE_API_URL`.

**2. Repository su GitHub** con `be/`, `fe/` e `render.yaml` nella radice.

**3. Render → New → Blueprint →** la repository → **Apply**. Nascono tre risorse:
il database, il backend Docker e il sito statico.

**4. Le variabili `sync: false`**, da riempire a mano — senza `/` finale:

| Servizio | Variabile | Valore | Quando |
|---|---|---|---|
| backend | `ADMIN_PASSWORD` | la password che si vuole per `admin@demo.it` | anche dopo: il seeder la riallinea a ogni avvio |
| backend | `ALLOWED_ORIGIN` | `https://radici-vivaio-fe.onrender.com` | dopo la prima build |
| frontend | `VITE_API_URL` | `https://radici-vivaio-be.onrender.com` | dopo la prima build |

`JWT_SECRET` non va toccata: la genera Render (`generateValue: true`) e non compare mai
nella repository.

**5. Manual Deploy di entrambi** i servizi. Per il frontend è obbligatorio: Vite sostituisce
`VITE_API_URL` durante la **build**, non a runtime.

**6. Le tabelle** le crea Hibernate (`ddl-auto: update`) e il seeder carica utenti e catalogo
al primo avvio: su Render nessuno lancia `CREATE TABLE` a mano.

> Il piano free sospende il servizio dopo ~15 minuti di inattività: la prima richiesta dopo
> una pausa può metterci una trentina di secondi. Non è un errore.

## Gli agenti di sicurezza

`be/src/test/.../agents/` contiene i cinque agenti di U7W1D2 (Mappa, SQLi, XSS, CSRF, IDOR).
Gli interruttori `app.vulnerable.*` in `application.yml` sono tutti a **`false`**: online gira
la versione difesa. Per rifare la fase "attacco" in locale si rimettono a `true` e si
rilanciano gli agenti — il Dockerfile builda con `-DskipTests`, quindi non toccano il deploy.
