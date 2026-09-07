# PROJECT_PROGRESS

Actualizat: 2026-09-08. Etapa curentă: **Faza 4 – Eureka Server + OpenFeign + Service Discovery finalizată cu succes**. Monolitul rămâne 100% stabil și funcțional (tag git: `monolith-stable`, 219 teste Java PASS).
Total teste Java active în întreg repo: **343 teste PASS** (Monolit: 219, Eureka Server: 1, user-service: 16, account-service: 44, transaction-service: 63).
Toate cele 3 microservicii (`user-service`: 8081, `account-service`: 8082, `transaction-service`: 8083) sunt înregistrate dinamic în `eureka-server` (8761) și comunică inter-servicii declarativ prin Spring Cloud OpenFeign cu propagare automată de Bearer JWT token și decodare uniformă a erorilor HTTP, fără URL-uri hardcodate în logica de business.

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
- [x] Eureka
- [x] OpenFeign
- [x] JWT/distributed security (emitere RS256 in user-service, validare JWKS in account-service & transaction-service, propagare automata Bearer token prin Feign RequestInterceptor)
- [ ] Gateway
- [ ] Load balancing (multi-instance)
- [ ] Resilience4j
- [ ] Actuator (integrat pe toate serviciile; urmeaza metrici avansate Prometheus)
- [ ] Prometheus
- [ ] Grafana
- [ ] Redis
- [ ] Config Server
- [ ] Saga
- [ ] Docker
- [ ] Docker Compose
- [ ] Deployment

---

## Faza 4 – Eureka Server + OpenFeign + Service Discovery (finalizată)

Data finalizării: 2026-09-08.
S-a implementat complet arhitectura de **Service Discovery** și **Comunicare Inter-servicii Declarativă** folosind trenul de release **Spring Cloud 2025.1.3 (Oakwood)**, compatibil cu Spring Boot 4.0.5 și Java 25.

### 1. Componente Implementate

1. **`eureka-server` (Port 8761):**
   - Modul independent `eureka-server/` cu build Gradle de sine stătător.
   - Configurat cu `@EnableEurekaServer`, funcționare în mod standalone (`register-with-eureka=false`, `fetch-registry=false`).
   - Actuator integrat (`/actuator/health`, `/actuator/info`).
   - Test de context load și pornire registry verificat (`EurekaServerApplicationTests`).

2. **Înregistrare Clienți Eureka:**
   - **`user-service` (Port 8081):** `@EnableDiscoveryClient`, înregistrare automată în Eureka (`USER-SERVICE`), raportare stare instanță `UP`.
   - **`account-service` (Port 8082):** `@EnableDiscoveryClient`, `@EnableFeignClients`, înregistrare automată în Eureka (`ACCOUNT-SERVICE`).
   - **`transaction-service` (Port 8083):** `@EnableDiscoveryClient`, `@EnableFeignClients`, înregistrare automată în Eureka (`TRANSACTION-SERVICE`).

3. **Comunicație Inter-servicii Declarativă (OpenFeign):**
   - **`account-service` -> `user-service`:** Interfața `@FeignClient(name = "user-service", configuration = FeignClientConfig.class)` (`UserFeignClient`) rezolvă dinamic adresa serviciului de utilizatori prin Eureka, fără URL hardcodat.
   - **`transaction-service` -> `account-service`:** Interfața `@FeignClient(name = "account-service", configuration = FeignClientConfig.class)` (`AccountFeignClient`) rezolvă dinamic adresa serviciului de conturi prin Eureka pentru debitare, creditare, interogare sold, verificare acces cont și limite.
   - Eliminarea proprietăților de URL hardcodat (`user-service.url`, `account-service.url`) din logica de business; serviciile apelează exclusiv clienții Feign numiți.

4. **Propagare Securitate & Bearer Token (`FeignAuthInterceptor`):**
   - Interceptor dedicat Feign (`RequestInterceptor`) implementat în ambele servicii consumatoare.
   - Extrage antetul `Authorization: Bearer <token>` din contextul cererii HTTP curente (`RequestContextHolder` / `HttpServletRequest`).
   - Mecanism de fallback pe `SecurityContextHolder` (`JwtAuthenticationToken`) pentru operațiuni asincrone sau background jobs autentificate.
   - Asigură propagarea transparentă a identității utilizatorului apelant de-a lungul întregului lanț de apeluri inter-servicii.

5. **Tratare Centralizată a Erorilor Feign (`CustomFeignErrorDecoder`):**
   - Decodor personalizat `ErrorDecoder` pentru maparea controlată a răspunsurilor HTTP din microserviciile apelate:
     - `HTTP 404` -> `ResourceNotFoundException`
     - `HTTP 403` -> `AccessDeniedException`
     - `HTTP 400` -> `IllegalArgumentException`
     - `HTTP 503 / 504` -> `ServiceUnavailableException` (mapat la HTTP 503 prin `GlobalExceptionHandler`)
   - Previne propagarea excepțiilor brute `FeignException` către clienții externi.

### 2. Rezultate Teste și Verificare Live

- **Verificare Regresie Suită Completă (343 teste Java PASS):**
  - **Monolit (`proiect`):** **219 teste PASS, 0 failures, 0 skipped** (JaCoCo: 77.16%).
  - **`eureka-server`:** **1 test PASS, 0 failures, 0 skipped**.
  - **`user-service`:** **16 teste PASS, 0 failures, 0 skipped** (JaCoCo: 86.76%).
  - **`account-service`:** **44 teste PASS, 0 failures, 0 skipped** (JaCoCo: 70.69%).
  - **`transaction-service`:** **63 teste PASS, 0 failures, 0 skipped** (JaCoCo: 76.84%).
- **Verificare Live End-to-End (`verify_phase4_service_discovery.py`):**
  - Toate cele 4 componente au fost pornite concurent (`eureka-server` 8761, `user-service` 8081, `account-service` 8082, `transaction-service` 8083).
  - Registry-ul Eureka a raportat toate cele 3 instanțe `{'USER-SERVICE', 'ACCOUNT-SERVICE', 'TRANSACTION-SERVICE'}` cu status `UP`.
  - S-a executat fluxul E2E complet prin OpenFeign:
    1. Înregistrare utilizator Alpha pe `user-service` (201 Created).
    2. Autentificare Alpha -> obținere JWT RS256 (200 OK, ID=1).
    3. Înregistrare și autentificare utilizator Beta (ID=2).
    4. Creare conturi RON și EUR pe `account-service` folosind JWT (201 Created).
    5. Creare categorie pe `transaction-service` (201 Created).
    6. Transfer între conturi proprii (`transfer-own`) pe `transaction-service`: rezolvare dinamică Eureka către `account-service`, propagare token Bearer, debitare și creditare executate cu succes (status EXECUTED, solduri verificate: Acc1 = 900 RON, Acc2 = 300 RON).
    7. Plată urgentă (`initiate` URGENT) de 50 RON către Beta: debitare Acc1 (850 RON) și creditare Acc4 (550 RON) prin OpenFeign.
    8. Schimb valutar (`exchange`) RON -> EUR prin OpenFeign: sold Acc1 = 800 RON, sold Acc3 = 10.05 EUR.
    9. Interogare istoric tranzacții: 3 tranzacții înregistrate și paginate.
    10. Verificare securitate IDOR: Beta încearcă să acceseze istoricul tranzacțiilor din contul lui Alpha -> **403 Forbidden** ca așteptat.
    11. Verificare acces neautentificat: cerere fără token -> **401 Unauthorized** ca așteptat.
  - Oprire curată a tuturor proceselor.

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
