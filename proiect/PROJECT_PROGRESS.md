# PROJECT_PROGRESS

Actualizat: 2026-09-08. Etapa curentă: **Faza 5 – Load Balancing și Scalabilitate finalizată cu succes**. Monolitul rămâne 100% stabil și funcțional (tag git: `monolith-stable`, 219 teste Java PASS).
Total teste Java active în întreg repo: **361 teste PASS** (Monolit: 219, Eureka Server: 1, user-service: 18, account-service: 58, transaction-service: 65). Toate modulele depășesc pragul de 70% acoperire JaCoCo.
Toate cele 3 microservicii rulează scalat orizontal în regim multi-instanță (minim 2 replici active per microserviciu, 6 instanțe de business înregistrate dinamic în Eureka Server 8761). Distribuția traficului inter-servicii este gestionată nativ prin Spring Cloud LoadBalancer (Round-Robin pe clienții Feign). Persistența identității de semnare JWT RS256 este partajată și protejată concurențial, iar baza de date este partajată semantic per domeniu de date.

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
- [x] Load balancing (multi-instance) (Spring Cloud LoadBalancer RoundRobin, 6 replici active, persistență chei RSA partajate, failover verificat)
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
