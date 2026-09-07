# PROJECT_PROGRESS

Actualizat: 2026-09-08. Etapa curentă: **Faza 3 – Extragerea transaction-service finalizată cu succes**. Monolitul rămâne 100% stabil și funcțional (tag git: `monolith-stable`, 219 teste Java PASS).
`user-service` (port 8081), `account-service` (port 8082) și `transaction-service` (port 8083) sunt microservicii Spring Boot complet independente, cu propriile baze de date (`user_db`, `account_db` și `transaction_db`), build-uri proprii, securitate distribuită OAuth2 RS256 JWT, comunicare inter-servicii via `RestClient` cu propagare automată de Bearer token și acoperire JaCoCo >= 70%.

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
- [x] transaction-service
- [ ] Eureka
- [ ] OpenFeign
- [ ] Gateway
- [ ] Load balancing
- [ ] JWT/distributed security (in progres: emitere si validare Bearer JWT in user-service, account-service si transaction-service; urmeaza propagare la Gateway)
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

## Faza 3 – Extragere transaction-service (finalizată)

Data finalizării: 2026-09-08. S-a creat al treilea microserviciu de business independent: **`transaction-service`** (port 8083).
Monolitul preexistent este păstrat intact sub tag-ul git **`monolith-stable`** (219 teste Java PASS).
`user-service` (21 teste Java PASS) și `account-service` (37 teste Java PASS) rămân complet funcționale.

### 1. Granița Arhitecturală și Responsabilități
`transaction-service` este o aplicație Spring Boot 4 / Java 25 de sine stătătoare, localizată în `transaction-service/`.
- **Responsabilități:**
  - Inițiere plăți standard și urgente (`PaymentRequestDTO`), validare sold și limite de tranzacționare
  - Transferuri între conturi proprii (`transfer-own`) cu actualizare imediată a soldurilor
  - Schimb valutar (`exchange`) la curs de referință cu conversie exactă a sumelor
  - Gestiune categorii de cheltuieli (`Category`): categorii de sistem globale și categorii personalizate per utilizator, cu soft-delete (`status = INACTIVE`)
  - Plăți programate (`ScheduledPayment`) cu data viitoare de execuție
  - Joburi de fundal planificate (`@Scheduled`):
    - `PaymentJob`: procesare automată a plăților standard aprobate (`PENDING_EXECUTION`)
    - `ScheduledPaymentJob`: executare automată a plăților programate devenite scadente
    - `ExchangeRateUpdateJob`: sincronizare zilnică a parităților de schimb cu cursul oficial BNR (XML) și mecanism robust de fallback pe rate de rezervă preîncărcate
  - Paginare și sortare istoric tranzacții per utilizator și per cont bancar, cu mascare automată a contrapărții și categoriilor pentru utilizatorii neautorizați

### 2. Izolarea Datelor (Database Ownership)
- `transaction-service` deține în mod suveran și exclusiv schema **`transaction_db`** (tabelele `transactions`, `scheduled_payments`, `exchange_rates`, `categories`, `tags`, `transaction_tags`).
- **FĂRĂ relații JPA directe către Account sau User:** Niciun `@ManyToOne` sau `@OneToOne` către entitățile celorlalte servicii; sunt utilizați exclusiv identificatori scalari:
  - `Integer initiatedByUserId`
  - `Long sourceAccountId`
  - `Long destinationAccountId`
  - `Integer createdByUserId`
- **Snapshot-uri Denormalizate de Audit:** `sourceAccountIban`, `sourceAccountAlias`, `destinationAccountIban`, `destinationAccountAlias` sunt salvate direct pe tranzacție, făcând istoricul complet rezistent la modificările sau ștergerile ulterioare din `account-service`.
- **Precizie Financiară:** Toate sumele și ratele folosesc strict `BigDecimal` cu rotunjire `RoundingMode.HALF_UP` (scară 2 pentru sume, scară 6 pentru cursuri valutare). Nicio valoare `double` nu este utilizată.
- **Profiluri de bază de date:** `test` (H2 in-memory), `dev` (MySQL local `transaction_db`), `prod` (variabile de mediu).

### 3. Securitate Distribuită & Resursă OAuth2
- `transaction-service` este configurat ca **OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`).
- Validează token-urile JWT asimetrice (RS256) emise de `user-service` folosind endpoint-ul public JWKS (`http://localhost:8081/.well-known/jwks.json`).
- `CurrentUserService` extrage dinamic `userId`, `username`, `email`, `role` și Bearer token-ul brut din contextul de securitate.
- `ResourceAuthorizationService` verifică autorizarea operațiunilor apelând `account-service` (drepturi `PAY`, `READ`, `OWNER`) și verifică dreptul de proprietate asupra categoriilor.
- Utilizatorii fără drepturi primesc `HTTP 403 Forbidden` (prevenire IDOR cross-user și cross-account). Cererile fără token returnează `HTTP 401 Unauthorized`.

### 4. Comunicare Inter-servicii (`AccountClient`)
- Implementat prin Spring 6 `RestClient` cu URL configurabil (`account-service.url=http://localhost:8082`).
- Propagă automat antetul `Authorization: Bearer <token>` din cererea curentă către `account-service`.
- Oferă metode dedicate pentru debitare (`debit`), creditare (`credit`), detalii cont (`getAccount`), verificare acces (`checkAccess`), verificare limite (`getUserLimits`) și listare conturi active (`getUserAccounts`).

### 5. Rezultate Teste și Acoperire
- **Teste `transaction-service`:** **57 teste PASS, 0 failures, 0 skipped**.
- **Acoperire JaCoCo:** **77% instrucțiuni** (2.790 / 3.590), depășind pragul obligatoriu de 70%.
- **Test Live Inter-servicii (HTTP multi-serviciu complet):** Scriptul `verify_phase3_live_services.py` a rulat împotriva tuturor celor 3 servicii pornite simultan (`user-service` pe 8081, `account-service` pe 8082, `transaction-service` pe 8083):
  1. Înregistrare utilizator Alpha pe `user-service`: PASS (201 Created).
  2. Login Alpha pe `user-service` și extragere JWT RS256: PASS (200 OK, ID=1).
  3. Înregistrare utilizator Beta pe `user-service`: PASS (201 Created).
  4. Login Beta pe `user-service` și extragere JWT RS256: PASS (200 OK, ID=2).
  5. Creare cont principal RON (1000 sold) pentru Alpha pe `account-service`: PASS (201 Created).
  6. Creare cont secundar RON (200 sold) pentru Alpha pe `account-service`: PASS (201 Created).
  7. Creare cont EUR (0 sold) pentru Alpha pe `account-service`: PASS (201 Created).
  8. Creare cont RON (500 sold) pentru Beta pe `account-service`: PASS (201 Created).
  9. Creare categorie 'Utilitati' pe `transaction-service`: PASS (201 Created).
  10. Transfer între conturi proprii (100 RON de la Acc1 la Acc2) pe `transaction-service`: PASS (status EXECUTED, solduri verificate pe account-service: Acc1 = 900 RON, Acc2 = 300 RON).
  11. Inițiere plată standard (250 RON de la Alpha la Beta) pe `transaction-service`: PASS (status PENDING_EXECUTION).
  12. Inițiere plată urgentă (50 RON de la Alpha la Beta) pe `transaction-service`: PASS (status EXECUTED, solduri verificate: Acc1 = 850 RON, Beta Acc4 = 550 RON).
  13. Schimb valutar (49.75 RON -> EUR) pe `transaction-service`: PASS (status EXECUTED, sold Acc1 = 800.25 RON, Acc3 = 10.0 EUR).
  14. Paginare și interogare istoric tranzacții Alpha: PASS (totalElements = 4).
  15. Verificare securitate IDOR: Beta încearcă să acceseze tranzacțiile din contul lui Alpha: PASS (403 Forbidden).
  16. Verificare securitate: Cerere fără token la `transaction-service`: PASS (401 Unauthorized).
- **Verificare regresie Monolit:** **219 teste PASS, 0 failures, 0 skipped**.
- **Verificare regresie `user-service`:** **21 teste PASS, 0 failures, 0 skipped**.
- **Verificare regresie `account-service`:** **37 teste PASS, 0 failures, 0 skipped**.

---

## Faza 2 – Extragere account-service (finalizată)

Data finalizării: 2026-09-07. S-a creat al doilea microserviciu de business independent: **`account-service`** (port 8082).
Monolitul preexistent este păstrat intact sub tag-ul git **`monolith-stable`** (219 teste Java PASS).
`user-service` rămâne funcțional (21 teste Java PASS, expunere publică JWKS la `/.well-known/jwks.json`).
- Deține `account_db` (tabelele `accounts`, `account_access`, `cards`, `bank_limits`, `user_limits`).
- 37 teste trecute, ~80% JaCoCo coverage.

---

## Faza 1 – Extragere user-service (finalizată)

Data finalizării: 2026-09-07. S-a creat primul microserviciu de business independent: **`user-service`** (port 8081).
Monolitul preexistent este stabilizat și păstrat intact sub tag-ul git **`monolith-stable`** (219 teste Java, suită E2E, CSRF și Remember Me).
- Deține `user_db` (tabelele `users` și `individuals`).
- Autentificare JWT RS256, hashing BCrypt, expunere publică JWKS la `/.well-known/jwks.json`.
- 21 teste trecute, ~86% JaCoCo coverage.

---

## Planul Următorilor Pași (Faza 4: Infrastructură Distribuită și Integrare)

1. **Service Discovery:** Netflix Eureka (`eureka-server` pe port 8761).
2. **API Gateway:** Spring Cloud Gateway cu rutare unificată către porturile 8081, 8082, 8083 și validare Bearer token.
3. **Comunicație Declarativă:** Migrare apeluri RestClient la OpenFeign cu load balancing integrat.
4. **Reziliență & Tranzacții Distribuite:**
   - Resilience4j (Circuit Breaker, Retry, Rate Limiter).
   - Implementare completă Saga Orchestration cu tranzacții de compensare pentru transferuri și plăți.
5. **Observabilitate & Cache:**
   - Redis cache pentru rate de schimb și verificări frecvente.
   - Actuator, Prometheus și Grafana.
6. **Containerizare:** Dockerfile per microserviciu și `docker-compose.yml` pentru întreg clusterul.
