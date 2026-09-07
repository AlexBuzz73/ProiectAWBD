# PROJECT_PROGRESS

Actualizat: 2026-09-07. Etapa curentă: **Faza 2 – Extragerea account-service finalizată cu succes**. Monolitul rămâne 100% stabil și funcțional (tag git: `monolith-stable`, 219 teste Java PASS).
Atât `user-service` (port 8081) cât și `account-service` (port 8082) sunt microservicii Spring Boot complet independente, cu propriile baze de date (`user_db` și `account_db`), build-uri proprii, securitate distribuită OAuth2 RS256 JWT, comunicare inter-servicii via `RestClient` cu propagare de Bearer token și acoperire JaCoCo >= 70%.

## Checklist

- [x] Mandatory requirements
- [x] CSRF
- [x] Resource authorization (session identity / ownership)
- [x] Remember Me
- [x] Error handling
- [x] 404/500
- [x] Pagination
- [x] Coverage >=70%
- [x] README
- [x] user-service
- [x] account-service
- [ ] transaction-service
- [ ] Eureka
- [ ] OpenFeign
- [ ] Gateway
- [ ] Load balancing
- [ ] JWT/distributed security (in progres: emitere si validare Bearer JWT in user-service si account-service; urmeaza propagare la Gateway si transaction-service)
- [ ] Resilience4j
- [ ] Actuator
- [ ] Prometheus
- [ ] Grafana
- [ ] Redis
- [ ] Config Server
- [ ] Saga
- [ ] Docker
- [ ] Docker Compose
- [ ] Deployment

---

## Faza 2 – Extragere account-service (finalizată)

Data finalizării: 2026-09-07. S-a creat al doilea microserviciu de business independent: **`account-service`** (port 8082).
Monolitul preexistent este păstrat intact sub tag-ul git **`monolith-stable`** (219 teste Java PASS).
`user-service` rămâne funcțional (21 teste Java PASS, expunere publică JWKS la `/.well-known/jwks.json`).

### 1. Granița Arhitecturală și Responsabilități
`account-service` este o aplicație Spring Boot 4 / Java 25 de sine stătătoare, localizată în `account-service/` (cu junction în rădăcina repository-ului).
- **Responsabilități:**
  - Gestiune conturi bancare (`Account`), generare automată de IBAN valid (`ROxxBANK...`), sold inițial și alias
  - Sistem de autorizare multi-user și partajare cont (`AccountAccess`: `OWNER`, `CO_OWNER`, `VIEWER`)
  - Închidere cont (strict permisă doar pentru `OWNER`, doar pe sold zero și status `ACTIVE`)
  - Emitere și administrare carduri de debit (`Card`: 16 cifre, CVV 3 cifre, expirare 3 ani în viitor, status `ACTIVE`/`BLOCKED`/`CLOSED`)
  - Gestiune limite tranzacționale bancare (`BankLimit`) și per utilizator (`UserLimit`), cu validare că limitele de utilizator nu depășesc limitele băncii și fallback automat la limitele băncii când nu există limită specifică
  - Partajare cont administrativă (`SharedAccountRequest`) pentru maxim 2 utilizatori, validând existența utilizatorilor prin apel REST securizat către `user-service`
  - Agregare solduri per valută (`/api/accounts/summary/currency`) și paginare conturi (`/api/accounts/paged`)

### 2. Izolarea Datelor (Database Ownership)
- `account-service` deține în mod suveran și exclusiv schema **`account_db`** (tabelele `accounts`, `account_access`, `cards`, `bank_limits`, `user_limits`).
- **FĂRĂ relații JPA directe către User:** Relația `@ManyToOne User user` a fost înlocuită complet cu scalarul `Integer userId` în `AccountAccess` și `UserLimit`.
- Nu există foreign keys către `user_db`.
- Precizie financiară: Toate sumele și limitele folosesc `BigDecimal` (precizie 19, scară 4).
- Blocare optimistă: `@Version private Long version;` pe entitatea `Account`.
- Profiluri de bază de date: `test` (H2 in-memory), `dev` (MySQL local `account_db`), `prod` (variabile de mediu).

### 3. Securitate Distribuită & Resursă OAuth2
- `account-service` este configurat ca **OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`).
- Validează token-urile JWT asimetrice (RS256) emise de `user-service` folosind endpoint-ul public JWKS (`http://localhost:8081/.well-known/jwks.json`).
- `CurrentUserService` extrage dinamic identitatea utilizatorului (`userId`, `username`, `email`, `role`) direct din claim-urile JWT verificate.
- `ResourceAuthorizationService` aplică matricea strictă de permisiuni:
  - `OWNER`: Acces deplin (inclusiv emitere/ștergere carduri, închidere cont).
  - `CO_OWNER`: Acces operațional (vizualizare, emitere carduri, inițiere tranzacții; interzisă închiderea contului).
  - `VIEWER`: Acces read-only (interzise emiterea de carduri, ștergerea cardurilor și modificarea contului).
  - Utilizatorii fără acces sau cu rol insuficient primesc `HTTP 403 Forbidden`. Resursele inexistente returnează `HTTP 404 Not Found`.

### 4. Comunicare Inter-servicii (`UserClient`)
- Implementat prin Spring 6 `RestClient` cu URL configurabil (`user-service.url=http://localhost:8081`).
- Propagă automat antetul `Authorization: Bearer <token>` din cererea curentă către `user-service`.
- Validează existența utilizatorilor la partajarea contului; tratează HTTP 404 de la `user-service` transformându-l curat în `ResourceNotFoundException`.

### 5. Rezultate Teste și Acoperire
- **Teste `account-service`:** **37 teste PASS, 0 failures, 0 skipped**.
- **Acoperire JaCoCo:** **79.80% instrucțiuni** (2.232 / 2.797), **80.62% linii** (491 / 609), depășind pragul minim de 70%.
- **Test Live Inter-servicii (HTTP):** Scriptul `verify_live_services.py` a rulat împotriva `user-service` (port 8081) și `account-service` (port 8082):
  1. Validare JWKS RFC 7517 public: PASS.
  2. Înregistrare utilizator pe `user-service`: PASS (201 Created).
  3. Login pe `user-service` și obținere JWT RS256: PASS (200 OK).
  4. Creare cont pe `account-service` cu Bearer JWT: PASS (201 Created cu IBAN).
  5. Listare conturi pe `account-service`: PASS (200 OK, rol OWNER).
  6. Emitere card de debit pe `account-service`: PASS (201 Created, 16 cifre).
  7. Verificare limite pe `account-service` cu fallback: PASS (200 OK).
  8. Cerere fără token pe `account-service`: PASS (401 Unauthorized).
- **Verificare regresie `user-service`:** **21 teste PASS, 0 failures, 0 skipped**.
- **Verificare regresie Monolit:** **219 teste PASS, 0 failures, 0 skipped**.

---

## Faza 1 – Extragere user-service (finalizată)

Data finalizării: 2026-09-07. S-a creat primul microserviciu de business independent: **`user-service`** (port 8081).
Monolitul preexistent este stabilizat și păstrat intact sub tag-ul git **`monolith-stable`** (219 teste Java, suită E2E, CSRF și Remember Me).
- Deține `user_db` (tabelele `users` și `individuals`).
- Autentificare JWT RS256, hashing BCrypt, expunere publică JWKS la `/.well-known/jwks.json`.
- 21 teste trecute, ~86% JaCoCo coverage.

---

## Planul Următorilor Pași (Faza 3: transaction-service)

Microserviciile rămase **NU au fost începute** în această etapă:
1. **`transaction-service` (Faza 3 următoare):**
   - Extragere plăți standard și urgente, transferuri proprii, plăți programate, schimb valutar, categorii și etichete.
   - Deținerea schemei `transaction_db`.
   - Bounded context: `Transaction`, `ScheduledPayment`, `Category`, `Tag`, `ExchangeRate`.
   - FĂRĂ relații JPA directe către Account sau User (folosire `Long accountId` și `Integer userId` scalari).
   - Validare JWT ca OAuth2 Resource Server.
   - Apeluri inter-servicii către `account-service` pentru verificare sold și debitare/creditare conturi.
2. **Infrastructură Distribuită (Faza 4):**
   - Eureka Service Discovery, Spring Cloud Gateway, Config Server, Resilience4j, Saga Pattern.
