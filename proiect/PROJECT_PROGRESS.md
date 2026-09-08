# PROJECT_PROGRESS

Actualizat: 2026-09-08. Etapa curentă: **Faza 8 – Design Pattern: Strangler Fig finalizată cu succes**. Monolitul rămâne 100% stabil și funcțional (tag git: `monolith-stable`, 219 teste Java PASS).
Total teste Java active în întreg repo: **391 teste PASS** (Monolit: 219, Eureka Server: 1, user-service: 18, account-service: 66, transaction-service: 74, gateway-service: 13). Toate modulele depășesc pragul de 70% acoperire JaCoCo (Gateway: 94.90%, User: 77.95%, Transaction: 77.08%, Monolit: 77.16%, Account: 75.46%).
Proiectul demonstrează implementarea completă a **Strangler Fig Pattern** prin migrarea incrementală a domeniilor de business din monolit în microservicii independente (`user-service`, `account-service`, `transaction-service`), dirijate transparent prin `gateway-service` (Strangler Facade pe Port 8090) și protejate prin Resilience4j. Monolitul original este menținut funcțional și testabil.

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
- [x] Gateway
- [x] Load balancing (multi-instance) (Spring Cloud LoadBalancer RoundRobin, 6 replici active, persistență chei RSA partajate, failover verificat)
- [x] Resilience4j (Circuit Breaker, Retry safe-reads, Fallbacks 503, Fail-Fast <50ms, Invariant Siguranță Financiară)
- [x] Actuator (integrat pe toate serviciile: health, info, circuitbreakers, circuitbreakerevents, retries, retryevents)
- [x] Design Pattern — Strangler Fig (Strangler Fig Pattern implemented and demonstrated through incremental migration from the stable monolith to three independently deployable business microservices.)
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

---

## Faza 5 – Load Balancing și Scalabilitate (finalizată)

Data finalizării: 2026-09-08.
S-a implementat și verificat complet cerința de **Load Balancing și Scalabilitate Orizontală** folosind **Spring Cloud LoadBalancer** (Spring Cloud 2025.1.3 / Oakwood), integrat nativ cu Spring Cloud OpenFeign și Eureka Service Discovery.

### 1. Arhitectură Multi-Instanță și Scalabilitate

Sistemul rulează cu minim 2 replici active pentru fiecare microserviciu:
- **`eureka-server`** (Port 8761): Registry centralizat de servicii.
- **`user-service`** (Replică A: Port 8081, Replică B: Port 8181): Emitent JWT RS256, partajare identitate criptografică RSA și persistență concurențială a utilizatorilor (`user_db`).
- **`account-service`** (Replică A: Port 8082, Replică B: Port 8182): Resource Server OAuth2, gestiune conturi, carduri și limite, bază de date partajată semantic (`account_db`).
- **`transaction-service`** (Replică A: Port 8083, Replică B: Port 8183): Resource Server OAuth2, motor tranzacții și plăți, client Feign către conturi, bază de date partajată semantic (`transaction_db`).

Fiecare replică își declară un identificator unic în Eureka: `eureka.instance.instance-id=${spring.application.name}:${server.port}`, permițând monitorizarea individuală a instanțelor UP.

### 2. Componente Implementate

1. **Replicabilitatea Cheii RSA în `user-service`:**
   - S-a rezolvat problema rotirii/generării independente a cheilor la restart/multi-instanță: toate replicile de `user-service` partajează aceeași pereche de chei RSA (2048-bit) persistată în `./keys/` sau configurabilă prin variabilă de mediu / fișier PEM extern.
   - Generarea este protejată concurențial prin scriere atomică (`StandardOpenOption.CREATE_NEW`) și mecanism de fallback pe citire concurențială (`FileAlreadyExistsException`), garantând că ambele instanțe expun seturi JWKS absolut identice (`kid=user-service-rsa-key-1`).
   - Private key-urile și bazele de date sunt complet excluse din git prin `.gitignore`.

2. **Spring Cloud LoadBalancer (`RoundRobinLoadBalancer`):**
   - Dependența `spring-cloud-starter-loadbalancer` a fost inclusă explicit în `account-service` și `transaction-service`.
   - Distribuția cererilor se realizează prin algoritmul Round-Robin implementat de `RoundRobinLoadBalancer` și `DiscoveryClientServiceInstanceListSupplier`.
   - TTL-ul cache-ului local de instanțe a fost configurat la `spring.cloud.loadbalancer.cache.ttl=2s` pentru detecție rapidă a modificărilor de topologie și failover eficient.

3. **Identificare și Trasabilitate Instanțe (`InstanceIdFilter`):**
   - Implementat filtru HTTP `InstanceIdFilter` pe toate cele 3 microservicii:
     - Header `X-Instance-Id`: `<service-name>:<port>`
     - Header `X-Service-Port`: `<port>`
     - Contribuitor Actuator `InfoContributor` la `/actuator/info` expunând metadatele instanței.

4. **Persistență Partajată Semantică (Multi-JVM Concurrency):**
   - Pentru scenariul de dezvoltare și rulare multi-instanță, profilele `discovery` utilizează baze de date cu suport concurențial de partajare `AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1` (sau MySQL în producție).
   - Entitățile păstrează protecția `@Version` pentru prevenirea lost-update prin Optimistic Locking la acces concurențial între replici.

5. **Endpoint-uri dedicate de testare Load Balancing:**
   - `account-service`: `GET /api/internal/accounts/feign-test/lb` apelează `user-service` prin Feign + LoadBalancer.
   - `transaction-service`: `GET /api/internal/transactions/feign-test/lb` apelează `account-service` prin Feign + LoadBalancer.

### 3. Rezultate Teste și Verificare Live

- **Suită de Teste Automate (361 teste Java PASS, JaCoCo > 70% peste tot):**
  - **Monolit:** 219 teste PASS (JaCoCo: 77.16%)
  - **`eureka-server`:** 1 test PASS
  - **`user-service`:** 18 teste PASS (JaCoCo: 77.95%) — include `UserJwtKeyReplicationTest` validând egalitatea JWKS și cross-verificarea semnăturilor între instanțe.
  - **`account-service`:** 58 teste PASS (JaCoCo: 73.44%) — include `LoadBalancerConfigurationTest` și `InternalAccountControllerTest`.
  - **`transaction-service`:** 65 teste PASS (JaCoCo: 75.67%) — include `LoadBalancerConfigurationTest` și interogări Feign.
- **Verificare Live Multi-Instanță (`verify_phase5_load_balancing.py`):**
  - Pornire concurentă a 7 procese (Eureka Server + 6 replici de microservicii).
  - Verificare Eureka: toate cele 6 instanțe raportate `UP` în registry (`USER-SERVICE: 2/2`, `ACCOUNT-SERVICE: 2/2`, `TRANSACTION-SERVICE: 2/2`).
  - Verificare JWKS: `user-service:8081` și `user-service:8181` servesc chei publice JWKS 100% identice.
  - Verificare antete: `X-Instance-Id` și `X-Service-Port` verificate pe toate cele 6 replici.
  - Verificare Load Balancing `account-service` -> `user-service`: 12 cereri consecutive distribuite strict alternant între porturile 8081 și 8181.
  - Verificare Load Balancing `transaction-service` -> `account-service`: 12 cereri consecutive distribuite strict alternant între porturile 8082 și 8182.
  - Verificare Failover: oprirea controlată a instanței `account-service:8082`; cererile transmise din `transaction-service` s-au rutat transparent către replica rămasă activă (`8182`), fără întreruperea serviciului.
  - Verificare E2E Multi-Replica:
    1. Înregistrare utilizator pe Replică B (`user-service:8181`).
    2. Autentificare pe Replică A (`user-service:8081`) folosind baza partajată și cheile partajate -> obținere JWT.
    3. Creare cont curent (1500 RON) pe Replică A (`account-service:8082`).
    4. Creare cont economii (500 RON) pe Replică B (`account-service:8182`).
    5. Verificare accesibilitate imediată cont 1 pe Replică B (bază partajată).
    6. Inițiere transfer 300 RON prin Replică A (`transaction-service:8083`) către `account-service` prin Feign + LoadBalancer.
    7. Verificare solduri actualizate pe Replică B (`account-service:8182`): Cont 1 = 1200 RON, Cont 2 = 800 RON.
  - Oprire curată a tuturor proceselor.

---

## Faza 6 – Spring Cloud API Gateway (finalizată)

Data finalizării: 2026-09-08.
S-a implementat și verificat complet cerința de **API Gateway** din barem folosind **Spring Cloud Gateway WebFlux** (`spring-cloud-starter-gateway-server-webflux`, Spring Cloud 2025.1.3 / Oakwood), integrat cu Spring Security WebFlux (OAuth2 Resource Server), Spring Cloud Netflix Eureka Client și Spring Cloud LoadBalancer.

### 1. Componente Implementate

1. **Modulul `gateway-service` (Port 8090):**
   - Modul Gradle independent (`proiect/gateway-service/`) cu `build.gradle`, `settings.gradle` și wrapper propriu.
   - Stack complet reactiv (Spring WebFlux + Netty, fără servlete blocante).
   - Înregistrat ca Eureka Client (`GATEWAY-SERVICE`).

2. **Rutare Dinamică Centralizată (`GatewayRoutesConfig`):**
   - Rutare automată bazată pe Service Discovery folosind prefixul `lb://<service-name>`:
     - `/api/auth/**`, `/.well-known/jwks.json`, `/api/users/**` -> `lb://user-service`
     - `/api/accounts/**`, `/api/limits/**` -> `lb://account-service`
     - `/api/transactions/**`, `/api/payments/**`, `/api/categories/**`, `/api/tags/**` -> `lb://transaction-service`
   - Rutare explicită, fără coliziuni, pentru rutele administrative:
     - `/api/admin/users/**`, `/api/admin/unlock-user` -> `lb://user-service`
     - `/api/admin/accounts/**`, `/api/admin/bank-limits/**`, `/api/admin/create-shared-account` -> `lb://account-service`

3. **Securitate Distribuită la Nivel de Gateway (`SecurityConfig`):**
   - Resource Server reactiv bazat pe JWT RS256 (`NimbusReactiveJwtDecoder` conectat la JWKS `http://localhost:8081/.well-known/jwks.json`).
   - Endpoint-uri publice permise fără autentificare: `/actuator/**`, `/api/auth/**`, `/.well-known/jwks.json`, `OPTIONS /**`.
   - Endpoint-uri administrative protejate strict: `/api/admin/**` necesită `ROLE_ADMIN` (extrase din claim-ul `role`).
   - Răspunsuri standardizate JSON: HTTP 401 Unauthorized (`AuthenticationEntryPoint`) și HTTP 403 Forbidden (`ServerAccessDeniedHandler`).
   - Propagare automată și neatinsă a antetului `Authorization: Bearer <token>` către microserviciile downstream (Defense-in-depth: microserviciile continuă să valideze independent token-urile).

4. **Rate Limiting In-Memory per IP (`InMemoryRateLimiter`, `RateLimiterGatewayFilterFactory`):**
   - Algoritm Token Bucket thread-safe fără dependențe externe.
   - Aplicat pe rute sensibile:
     - `/api/auth/login`: capacitate 10 token-uri, refill 2.0 token-uri/secundă.
     - `/api/auth/register`: capacitate 10 token-uri, refill 5.0 token-uri/secundă.
     - `/api/payments/**`: capacitate 15 token-uri, refill 5.0 token-uri/secundă.
   - Returnează HTTP 429 Too Many Requests cu antet `Retry-After: <seconds>` și corp JSON explicativ.

5. **Trasabilitate & Observabilitate (`CorrelationIdGlobalFilter`):**
   - Implementează `WebFilter` și `GlobalFilter` cu prioritate maximă (`Ordered.HIGHEST_PRECEDENCE`).
   - Generează un UUID nou dacă antetul `X-Correlation-Id` lipsește, sau propagă valoarea existentă trimisă de client.
   - Injectează antetele `X-Correlation-Id` și `X-Gateway-Service: gateway-service` în toate răspunsurile HTTP (inclusiv erori 401/403).
   - Logging structurat per cerere (metodă, cale, correlation ID, status HTTP, durată în ms).

6. **CORS Centralizat pentru Frontend (`CorsConfig`):**
   - `CorsWebFilter` cu `Ordered.HIGHEST_PRECEDENCE`.
   - Permite originile Vite `http://localhost:5173` și `http://127.0.0.1:5173`.
   - Permite toate metodele (`GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD`) și antetele cu `allowCredentials(true)`.
   - Expune antetele: `X-Correlation-Id`, `X-Gateway-Service`, `X-Instance-Id`, `X-Service-Port`, `Retry-After`.

7. **Integrare Frontend (`apiClient.js`):**
   - Suport nativ pentru Bearer token JWT extras din starea locală de autentificare (`getLoggedUser()`).
   - Ignorare automată CSRF la utilizarea JWT (CSRF rămâne activ doar pentru sesiunile monolitului).
   - Fișiere de configurare create: `.env.microservices` și `.env.example` indicând `VITE_API_BASE_URL=http://localhost:8090/api`.

### 2. Rezultate Teste și Verificare Live

- **Suită de Teste Automate (374 teste Java PASS, JaCoCo > 70% peste tot):**
  - **Monolit:** 219 teste PASS (JaCoCo: 77.16%)
  - **`eureka-server`:** 1 test PASS
  - **`user-service`:** 18 teste PASS (JaCoCo: 77.95%)
  - **`account-service`:** 58 teste PASS (JaCoCo: 73.44%)
  - **`transaction-service`:** 65 teste PASS (JaCoCo: 75.67%)
  - **`gateway-service`:** **13 teste PASS, 0 failures, 0 skipped** (**JaCoCo: 94.90%**)
  - **Frontend:** 6 teste unitare PASS, ESLint curat, Vite build cu succes.
- **Verificare Live End-to-End (`verify_phase6_gateway.py`):**
  - Rulare live cu 8 procese: Eureka (8761), 2x User (8081, 8181), 2x Account (8082, 8182), 2x Transaction (8083, 8183), Gateway (8090).
  - Toate cele 7 instanțe raportate `UP` în Eureka (`USER: 2/2, ACCOUNT: 2/2, TX: 2/2, GATEWAY: 1/1`).
  - **Securitate 401:** Cererea neautentificată la `/api/accounts` respinsă cu 401, primind `X-Correlation-Id` și `X-Gateway-Service: gateway-service`.
  - **Securitate JWT invalid:** Cererea cu token invalid respinsă cu 401.
  - **Rutare JWKS:** `GET http://localhost:8090/.well-known/jwks.json` returnează setul de chei publice (`kid=user-service-rsa-key-1`).
  - **CORS Preflight:** `OPTIONS http://localhost:8090/api/accounts` returnează HTTP 200 cu `Access-Control-Allow-Origin: http://localhost:5173` și `Access-Control-Allow-Credentials: true`.
  - **Înregistrare și Login:** Înregistrare la `POST http://localhost:8090/api/auth/register` (201 Created) și autentificare la `POST http://localhost:8090/api/auth/login` (200 OK) generând JWT valid.
  - **Autorizare RBAC 403:** Utilizatorul cu rol `USER` accesând `/api/admin/users/all` prin Gateway primește HTTP 403 Forbidden.
  - **Rate Limiting 429:** Rafala de 15 cereri rapide la `/api/auth/login` a declanșat HTTP 429 Too Many Requests cu antet `Retry-After: 1`.
  - **Acces Protejat prin Gateway:** `GET http://localhost:8090/api/accounts` cu Bearer token returnează HTTP 200 OK.
  - **Load Balancing prin Gateway (`user-service`):** 12 cereri la `/api/users/me` distribuite alternant între porturile 8081 și 8181.
  - **Load Balancing prin Gateway (`account-service`):** 12 cereri la `/api/accounts` distribuite alternant între porturile 8082 și 8182.
  - **Failover Transparent prin Gateway:** Oprirea forțată a replicii `account-service:8082`; Gateway-ul a redirecționat automat tot traficul către replica `8182` fără repornirea gateway-ului.
  - **Propagare Correlation ID:** Clientul trimite `X-Correlation-Id: custom-client-trace-998877`; valoarea este păstrată și returnată în răspunsul final.
  - **Rutare Transaction Service:** `GET http://localhost:8090/api/categories` rutat cu succes (HTTP 200).
  - Oprire curată a tuturor celor 8 procese.

---

## Faza 7 – Resilience4j & Fault Tolerance (finalizată)

Data finalizării: 2026-09-08.
S-a implementat și verificat complet cerința de **Fault Tolerance / Reziliență** din barem folosind **Resilience4j** (`spring-cloud-starter-circuitbreaker-resilience4j:5.0.3` / Resilience4j 2.3.0) și `spring-boot-starter-aspectj:4.0.5`, pe ambele relații critice de comunicare inter-servicii:
1. **Relația A:** `account-service` -> `user-service` (`UserFeignClient` / `UserClient`)
2. **Relația B:** `transaction-service` -> `account-service` (`AccountFeignClient` / `AccountClient`)

### 1. Arhitectură și Decizii Tehnice

1. **Aspect Order Deterministic:**
   - S-a configurat explicit ordinea aspectelor Spring AOP:
     - `resilience4j.circuitbreaker.circuitBreakerAspectOrder=1` (outer advice)
     - `resilience4j.retry.retryAspectOrder=2` (inner advice)
   - Când Circuit Breaker-ul este `OPEN`, apelurile sunt interceptate imediat de Circuit Breaker și executează fallback-ul fără ca Retry să mai intervină. Fail-fast se execută instantaneu (<15ms).
   - Când Circuit Breaker-ul este `CLOSED`, apelul este permis către Retry; în caz de eroare de rețea tranzitorie, Retry reîncearcă până la 3 ori înainte ca eroarea să fie înregistrată în sliding window-ul Circuit Breaker-ului.

2. **Configurație Circuit Breaker:**
   - Instanțe configurate: `userServiceCircuitBreaker` (în `account-service`) și `accountServiceCircuitBreaker` (în `transaction-service`).
   - `sliding-window-type=COUNT_BASED`
   - `sliding-window-size=10`
   - `minimum-number-of-calls=5`
   - `failure-rate-threshold=50%`
   - `wait-duration-in-open-state=10s`
   - `permitted-number-of-calls-in-half-open-state=2`
   - `automatic-transition-from-open-to-half-open-enabled=true`
   - Listeneri de evenimente configurate în `ResilienceConfig`:
     - Logare tranziții de stare: `[CIRCUIT-BREAKER] [{name}] State transition: {from} -> {to}`
     - Logare apeluri respinse: `[CIRCUIT-BREAKER] [{name}] Call NOT permitted (circuit is OPEN)`

3. **Politică de Retry & Invariant de Siguranță Financiară:**
   - **Operațiuni sigure de citire (Idempotente):** protejate cu `@Retry(name = "...")` + `@CircuitBreaker`:
     - `findUserById`, `findUserByEmail`, `getInstanceInfo` în `UserClient`
     - `getAccount`, `getAccountByIban`, `checkAccess`, `getUserLimits`, `getUserAccounts`, `getInstanceInfo` în `AccountClient`
     - Configurație Retry: `max-attempts=3`, `wait-duration=500ms`.
   - **MUTĂRI FINANCIARE (CRITIC):**
     - Metodele de mutație monetară (`debit`, `credit`, inițiere de plăți, transferuri) sunt protejate **EXCLUSIV de `@CircuitBreaker`** și **NU AU NICIODATĂ `@Retry`**.
     - Se elimină riscul de debite multiple accidentale în caz de timeout de rețea.
     - Dacă contul sau serviciul aval este indisponibil, tranzacția este abortată curat (`FAILED`), iar apelantul primește HTTP 503 fără operațiuni parțiale.

4. **Tratare Fallback & Contract Standardizat HTTP 503:**
   - Metodele de fallback re-aruncă excepțiile de business (`AccessDeniedException`, `IllegalArgumentException`, `ResourceNotFoundException`) pentru a păstra codurile corecte (403, 400, 404).
   - Erorile de infrastructură (`CallNotPermittedException`, timeout-uri, conexiuni refuzate) aruncă `ServiceUnavailableException`.
   - `GlobalExceptionHandler` tratează `ServiceUnavailableException` și `CallNotPermittedException`, injectând `X-Correlation-Id` și returnând HTTP 503:
     ```json
     {
       "error": "Service Unavailable",
       "message": "Serviciul de conturi nu este disponibil momentan.",
       "status": 503,
       "correlationId": "corr-fail-fast-tx"
     }
     ```
   - API Gateway (8090) transmite răspunsul 503 transparent către client, fără a-l masca în 500, păstrând antetele de trasabilitate.

5. **Expunere Actuator:**
   - Endpoint-uri expuse pe ambele microservicii: `/actuator/circuitbreakers`, `/actuator/circuitbreakerevents`, `/actuator/retries`, `/actuator/retryevents`.

### 2. Rezultate Teste și Verificare Live

- **Suită de Teste Automate (391 teste Java PASS, JaCoCo > 70% peste tot):**
  - **Monolit:** 219 teste PASS (JaCoCo: 77.16%)
  - **`eureka-server`:** 1 test PASS
  - **`user-service`:** 18 teste PASS (JaCoCo: 77.95%)
  - **`account-service`:** **66 teste PASS, 0 failures, 0 skipped** (**JaCoCo: 75.46%**) — include 10 teste dedicate în `UserServiceResilienceTest`.
  - **`transaction-service`:** **74 teste PASS, 0 failures, 0 skipped** (**JaCoCo: 77.08%**) — include 11 teste dedicate în `AccountServiceResilienceTest`.
  - **`gateway-service`:** 13 teste PASS (JaCoCo: 94.90%)
  - **Frontend:** 6 teste PASS, ESLint curat, Vite build cu succes.
- **Verificare Live Multi-Service (`verify_phase7_resilience.py`):**
  - Pornire concurentă a 7 instanțe: Eureka (8761), 2x User (8081, 8181), 2x Account (8082, 8182), 2x Transaction (8083, 8183), Gateway (8090).
  - **Pasul 1:** Stare inițială Actuator confirmată: `userServiceCircuitBreaker: CLOSED`, `accountServiceCircuitBreaker: CLOSED`.
  - **Pasul 2:** Flux de bază sănătos prin Gateway: Înregistrare, Login JWT, Creare cont 500 RON, interogări Feign între servicii cu status 200.
  - **Pasul 3 (Scenariul A — Cădere Totală `user-service`):**
    - Oprirea ambelor replici `user-service` (:8081 și :8181).
    - 6 apeluri eșuate declanșate din `account-service` -> `userServiceCircuitBreaker` a trecut în `OPEN`.
    - Apelul ulterior fail-fast a returnat HTTP 503 în doar **7ms** (fără blocaje sau apeluri inutile).
    - Repornire `user-service:8081`, expirare `wait-duration` (10s) -> trecere în `HALF_OPEN`.
    - Apelurile de probă au reușit -> Circuit Breaker a revenit în **`CLOSED`**.
  - **Pasul 4 (Scenariul B — Cădere Totală `account-service` & Siguranță Financiară):**
    - Oprirea ambelor replici `account-service` (:8082 și :8182).
    - 6 apeluri eșuate din `transaction-service` -> `accountServiceCircuitBreaker` a trecut în `OPEN`.
    - Apel fail-fast verificat în **15ms** cu HTTP 503.
    - **Audit siguranță financiară:** Tentativa de inițiere plată prin Gateway a fost respinsă imediat cu HTTP 503, prevenind orice execuție parțială sau corupere a soldurilor.
    - Repornire `account-service:8082`, apelurile de probă au reușit -> revenire în **`CLOSED`**.
  - **Pasul 5 (Scenariul C — Cădere Parțială Replică):**
    - Oprirea unei singure replici (`account-service:8082`, lăsând activă replica :8182).
    - Trimitere 8 cereri din `transaction-service`: Load Balancer-ul a rutat cererile către replica rămasă (7/8 cereri reușite imediat).
    - Circuit Breaker a rămas **`CLOSED`** (nu s-a deschis inutil pe cădere parțială).
  - **Pasul 6 (Regresie E2E după revenire):**
    - Interogare conturi și categorii prin Gateway: HTTP 200 OK.
  - Oprire curată a tuturor proceselor.

---

## Faza 8 – Design Pattern: Strangler Fig (finalizată)

Data finalizării: 2026-09-08.
Auditul de arhitectură confirmă implementarea formală și completă a **Strangler Fig Pattern** (Martin Fowler) prin migrarea incrementală a aplicației din monolit în microservicii.

> "Strangler Fig Pattern implemented and demonstrated through incremental migration from the stable monolith to three independently deployable business microservices."

### 1. Problema și Justificarea
- **Problema:** Aplicația a debutat ca un monolit cuprinzător (12 entități JPA, 219 teste automate, fluxuri financiare integrate). O abordare de tip "big-bang rewrite" (rescrierea dintr-o dată a întregului sistem) ar fi prezentat riscuri operaționale inacceptabile: perioade prelungite de instabilitate, regresii greu de izolat, imposibilitatea validării parțiale și downtime în producție.
- **Soluția Strangler Fig:** Păstrarea monolitului intact și stabil ca plasă de siguranță (`monolith-stable`), în timp ce domeniile de business (Bounded Contexts) sunt extrase incremental în microservicii independente. La final, un API Gateway (`gateway-service` :8090) preia rolul de Strangler Facade, dirijând tot traficul clienților către noile servicii, permițând retragerea ulterioară a monolitului fără întreruperea funcționării.

### 2. Etapele de Migrare Incrementală și Dovezi Git
1. **Baseline Stabil:** Commit `76647b8` (tag git `monolith-stable`) — Monolitul original este stabil, complet funcțional și acoperit de 219 teste Java PASS.
2. **Pasul 1 (User Domain):** Commit-urile `a04782a` & `e1eb02a` — Extragerea `user-service` (:8081/:8181) cu propria bază de date (`user_db`), autentificare JWT RS256 și endpoint public JWKS. Monolitul rămâne neatins.
3. **Pasul 2 (Account Domain):** Commit-urile `26603d1`, `8d48491`, `c5252c6` — Extragerea `account-service` (:8082/:8182) cu `account_db`, OAuth2 Resource Server și clienți Feign către `user-service`.
4. **Pasul 3 (Transaction Domain):** Commit `ba2a01e` — Extragerea `transaction-service` (:8083/:8183) cu `transaction_db`, OAuth2 Resource Server și clienți Feign către `account-service`.
5. **Pasul 4 & 5 (Discovery & Scalabilitate):** Commit-urile `d5df06e` & `ca9689d` — Integrarea Eureka Server (:8761) și Spring Cloud LoadBalancer pentru replicare multi-instanță.
6. **Pasul 6 (Strangler Facade):** Commit `b6e72a3` — Implementarea `gateway-service` (:8090) pe post de Strangler Facade: tot traficul extern este rutat exclusiv către noile microservicii (`lb://`), ascunzând detaliile interne de clienți.
7. **Pasul 7 (Reziliență Distribuită):** Commit `3ff8a8c` — Integrare Resilience4j Circuit Breaker & Retry pe relațiile inter-servicii.

### 3. Diagrame Arhitecturale de Migrare

```
FAZA INIȚIALĂ (Baseline Monolit)

Frontend:5173
      |
      v
  Monolith (port 8080)
      |
      v
  Monolith DB (H2/MySQL)


MIGRARE INCREMENTALĂ (Strangler Fig Facade)

                      +-----------------> user-service (:8081/:8181) [user_db]
                      |
Frontend -> Gateway --+-----------------> account-service (:8082/:8182) [account_db]
            (:8090)   |
                      +-----------------> transaction-service (:8083/:8183) [tx_db]

Monolith (tag: monolith-stable, 219 teste PASS)
   |
   +--> Păstrat ca referință stabilă și validare de paritate funcțională


ȚINTĂ FINALĂ (Decomisionare Monolit)

Frontend:5173
      |
      v
  Gateway (:8090)
      +---> user-service (independent)
      +---> account-service (independent)
      +---> transaction-service (independent)

  (Monolitul legacy poate fi arhivat/decomisionat fără impact asupra clienților)
```

### 4. Trade-Offs Analizate
- **Avantaje:**
  - Risc operațional redus masiv prin pași mici și reversibili.
  - Regresii ușor detectabile la nivelul fiecărui modul (regresia generală a rămas 100% verde pe tot parcursul).
  - Fiecare microserviciu este deployabil și scalabil independent.
  - Bounded contexts delimitate strict conform principiilor Domain-Driven Design (DDD).
  - Rollback imediat în caz de anomalie (monolitul a rămas complet funcțional).
- **Dezavantaje / Costuri:**
  - Duplicare temporară de cod și modele de date între monolit și noile servicii pe durata migrației.
  - Coexistența a două paradigme arhitecturale (monolit monolitic cu sesiune DB vs microservicii stateless cu JWT).
  - Necesitatea sincronizării contractelor de date (DTO-uri, formate JSON) între monolit și noile servicii.
  - Complexitate operațională crescută pe durata migrației (rulare concurentă de multiple procese).



