# PROJECT_PROGRESS

Actualizat: 2026-09-07. Etapa curenta: Validare, Error Handling, Remember Me si Documentatie finalizate. Toate cerintele mandatare pentru aplicatia de baza (monolit) sunt complet bifate si verificate.
Migrarea la microservicii nu a inceput. Bifele indica verificari efectuate, nu doar existenta unor fisiere.

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
- [ ] user-service
- [ ] account-service
- [ ] transaction-service
- [ ] Eureka
- [ ] OpenFeign
- [ ] Gateway
- [ ] Load balancing
- [ ] JWT/distributed security
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

## Etapa 3 – Validare, Error Handling, Remember Me și Documentație (finalizată)

Data verificarii: 2026-09-07. S-au implementat si verificat tratarea unitara a erorilor HTTP, validarea completa pe DTO-uri si controller-e, mecanismul Remember Me (backend + frontend), ErrorBoundary si pagina 404 in frontend, documentatia exhaustiva in README.md (cu diagrama ER Mermaid si catalog complet de API) si curatarea proprietatilor de productie.

### 1. Validare și Tratare Erori (HTTP 400, 401, 403, 404, 405, 500)

- **DTO Bean Validation:**
  - `CategoryRequestDTO`: adaugat `@NotBlank` si `@Size(min = 1, max = 50)` pe numele categoriei.
  - `SharedAccountRequest` si `UserRoleDTO`: adaugat `@NotBlank`, `@Pattern` pentru moneda (RON/EUR/USD), `@NotEmpty` si `@Size(min = 1, max = 2)` pentru lista de utilizatori, `@Email` si `@Pattern` pentru roluri (`OWNER|CO_OWNER|VIEWER`), plus adnotarea `@Valid` pe colectia de roluri.
  - `UserLimitRequestDTO`: adaugat `@NotNull` si `@DecimalMin(value = "0.01")` pe limitele financiare, `@Min(1)` pe numarul de tranzactii.
  - `AdminController.createSharedAccount`: adaugat `@Valid` la intrarea metodei.
- **GlobalExceptionHandler unificat:**
  - **404 Not Found:** Tratare explicita pentru `ResourceNotFoundException`, `NoResourceFoundException` si `NoHandlerFoundException`, returnand JSON `{"error": "Resursa nu a fost gasita"}` (rezolva problema deschisa anterioara unde `/api/does-not-exist` returna 500).
  - **405 Method Not Allowed:** Tratare explicita pentru `HttpRequestMethodNotSupportedException` -> 405 `{"error": "Metoda HTTP nu este suportata"}`.
  - **415 Unsupported Media Type:** Tratare explicita pentru `HttpMediaTypeNotSupportedException` -> 415.
  - **400 Bad Request:** Tratare consistenta pentru `MethodArgumentNotValidException` (campuri si mesaje de validare detaliate), `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`, `ConstraintViolationException`, `HandlerMethodValidationException`, `IllegalStateException`.
  - **403 Forbidden:** Pastrata maparea pentru acces neautorizat si IDOR cross-user.
  - **500 Internal Server Error:** Excepțiile neprevazute sunt capturate generic returnand JSON `{"error": "A aparut o eroare neasteptata. Va rugam incercati din nou."}`, fara a expune stacktrace-ul intern catre client.
- **Frontend Error Handling:**
  - Creat `ErrorBoundary.jsx` (React Class Component) ce captureaza exceptiile neprevazute din arborele React si afiseaza o interfata prietenoasa de recuperare cu buton de reincarcare.
  - In `App.jsx`, rutele aplicatiei au fost infasurate in `ErrorBoundary`.
  - Actualizat `NotFoundPage.jsx` cu design stil card dedicat si navigare dinamica spre Dashboard sau Login in functie de starea de autentificare.

### 2. Implementare Remember Me

- **Backend:**
  - Extins `LoginRequestDTO` cu proprietatea `Boolean rememberMe` si constructori adecvati.
  - In `SecurityConfig`:
    - Definit cheia de semnare `REMEMBER_ME_KEY`.
    - Configurat `TokenBasedRememberMeServices` cu valabilitate 14 zile (1.209.600 secunde) si cookie `remember-me` (HTTP-only).
    - Suport REST: `rememberMeRequested` detecteaza atributul de cerere `"rememberMe"` (sau parametrul URL), permitand compatibilitatea cu autentificarea REST JSON.
    - Inregistrat `RememberMeAuthenticationProvider` si configurat `.rememberMe(...)` pe `HttpSecurity`.
    - La logout (`POST /api/auth/logout`), configurat `deleteCookies("JSESSIONID", "remember-me")` si invalidare explicita.
  - In `AuthController`: injectat `RememberMeServices`, detecteaza `dto.getRememberMe() == true`, seteaza atributul pe cerere si apeleaza `rememberMeServices.loginSuccess(request, servletResponse, authentication)`.
- **Frontend:**
  - Adaugat checkbox *"Ține-mă minte (Remember Me)"* in `LoginForm.jsx` legat de starea `formData.rememberMe`.
  - Transmisie parametru `rememberMe` prin `authApi.loginUser`.

### 3. Documentație Mandatară și Curățare Configurație

- **README complet:**
  - Creat `README.md` atat in root (`/README.md`), cat si in modulul backend (`/proiect/README.md`).
  - Documentata arhitectura (Spring Boot 4, Java 25, React 18, Vite), diagrama ER Mermaid completa (toate cele 12 entitati JPA si relatiile 1:1, 1:N, N:M), catalogul complet de 36 endpoint-uri REST cu roluri si coduri de raspuns, specificatia de securitate (sesiuni, CSRF, Remember Me, prevenire IDOR, RBAC), configuratia celor 3 profiluri DB (`test`, `dev`, `prod`), instructiunile de rulare/testare si rezultatele JaCoCo.
- **Curatare `application-prod.properties`:**
  - Eliminate referintele la `data-test.sql` si flag-ul de depanare `RequestMappingHandlerMapping=TRACE`.

### 4. Teste Noi și Verificare Executată

- **Java Tests:**
  - `ValidationAndErrorHandlingIntegrationTest.java`: **12 teste MockMvc** verificand:
    - 404 pentru URL negasit / endpoint inexistent.
    - 401 pentru acces anonim la rute protejate.
    - 400 pentru validare nume categorie gol (`@NotBlank`).
    - 400 pentru limite de utilizator invalide (`@DecimalMin`).
    - 400 pentru cerere shared account cu lista goala, email invalid, rol invalid sau mai mult de 2 utilizatori.
    - 400 pentru body JSON malformat.
    - 405 pentru metoda HTTP nepermisa (`POST` pe endpoint `GET`).
    - 403 pentru incercare de inchidere cont de catre non-owner.
  - `RememberMeIntegrationTest.java`: **5 teste MockMvc** verificand:
    - Setarea cookie-ului `remember-me` la login cu `rememberMe = true`.
    - Absenta cookie-ului `remember-me` cand `rememberMe = false`.
    - Auto-login si acces autorizat la `/api/accounts` furnizand **doar** cookie-ul `remember-me` (fara `JSESSIONID`).
    - Stergerea cookie-urilor la logout.
    - Respingerea cookie-ului Remember Me invalid/alterat.
- **Frontend Tests:**
  - `tests/authAndErrors.test.js`: **2 teste Node.js** verificand propagarea corecta a flag-ului `rememberMe` la `loginUser` si propagarea erorilor server (400/401/403/404/500).
  - `e2e/rememberMeAndErrors.spec.js`: **1 scenariu complet Playwright E2E** ce inregistreaza un utilizator nou, face login cu Remember Me, sterge `JSESSIONID`, acceseaza un endpoint protejat exclusiv prin `remember-me` primind 200, apoi verifica pagina 404 in frontend si 404 pe URL backend necunoscut.

### 5. Rezultate Finale Verificare

| Comandă / Verificare | Rezultat |
|---|---|
| `.\gradlew.bat clean test` | **BUILD SUCCESSFUL**, **219 teste**, 0 eșecuri, 0 erori, 0 ignorate |
| `.\gradlew.bat clean test jacocoTestReport build` | **BUILD SUCCESSFUL**, **219 teste**; JAR și rapoarte JaCoCo generate |
| `npm run lint` | **PASS**, 0 erori, 0 avertismente |
| `npm test` | **PASS**, 5 teste unitare |
| `npm run build` | **PASS**, build Vite de producție reușit |
| `npm run test:e2e` | **PASS**, **5 scenarii Playwright** în Microsoft Edge headless |
| CSRF & Remember Me | Ambele mecanisme funcționale simultan, validate în teste automate și E2E |

**Acoperire JaCoCo (`jacocoTestReport.xml`):**
- Total Aplicație: **77.16% instrucțiuni** (5.249 / 6.803), **78.15% linii** (1.098 / 1.405).
- `com.example.demo.services.impl`: **78.56% instrucțiuni** (3.334 / 4.244), **81.28% linii** (673 / 828).
- `com.example.demo.controllers`: **88.60% instrucțiuni**, **87.86% linii**.
- `com.example.demo.config`: **90.44% instrucțiuni**, **90.43% linii**.
- Toate cerințele de acoperire (minim 70%) sunt pe deplin respectate.

### 6. Fișiere Modificate în Etapa 3

- Backend Config & Controllers:
  - `src/main/java/com/example/demo/config/SecurityConfig.java` (Remember Me services, provider, HttpSecurity config, cookie clearance).
  - `src/main/java/com/example/demo/controllers/AuthController.java` (injectat RememberMeServices, procesare login).
  - `src/main/java/com/example/demo/controllers/AdminController.java` (`@Valid` pe `createSharedAccount`).
  - `src/main/java/com/example/demo/exceptions/GlobalExceptionHandler.java` (handlere 404, 405, 415, 400, 500 prietenos).
- Backend DTOs:
  - `src/main/java/com/example/demo/dto/LoginRequestDTO.java` (camp `rememberMe`).
  - `src/main/java/com/example/demo/dto/CategoryRequestDTO.java` (`@NotBlank`, `@Size`).
  - `src/main/java/com/example/demo/dto/SharedAccountRequest.java` (`@NotBlank`, `@Pattern`, `@NotEmpty`, `@Size`, `@Valid`).
  - `src/main/java/com/example/demo/dto/UserRoleDTO.java` (`@Email`, `@NotBlank`, `@Pattern`).
  - `src/main/java/com/example/demo/dto/UserLimitRequestDTO.java` (`@NotNull`, `@DecimalMin`, `@Min`).
- Backend Resources:
  - `src/main/resources/application-prod.properties` (curatat de data-test.sql si logging trace).
- Backend Tests:
  - `src/test/java/com/example/demo/integration/ValidationAndErrorHandlingIntegrationTest.java` (nou, 12 teste).
  - `src/test/java/com/example/demo/integration/RememberMeIntegrationTest.java` (nou, 5 teste).
- Frontend Components & Pages:
  - `frontend/src/components/common/ErrorBoundary.jsx` (nou).
  - `frontend/src/App.jsx` (invelit rutele in ErrorBoundary).
  - `frontend/src/components/auth/LoginForm.jsx` (checkbox Remember Me).
  - `frontend/src/pages/NotFoundPage.jsx` (UI 404 imbunatatit).
- Frontend Tests:
  - `frontend/tests/authAndErrors.test.js` (nou, 2 teste).
  - `frontend/e2e/rememberMeAndErrors.spec.js` (nou, 1 scenariu E2E).
- Documentatie:
  - `README.md` (root, rescris complet).
  - `proiect/README.md` (nou, complet).
  - `proiect/PROJECT_PROGRESS.md` (actualizat).

---

## Etapa 2 – Autorizarea resurselor pe baza sesiunii (istoric)

Data verificarii: 2026-09-07. Problema IDOR a fost remediata pentru endpoint-urile HTTP. SecurityConfig, apiFetch si mecanismul CSRF din etapa 1 au fost pastrate.

### Design

- `CurrentUserService` citeste Authentication din SecurityContextHolder, respinge contextul anonim si incarca User prin email-ul principalului din UserRepository. Un utilizator DB absent/inactiv primeste 403; autentificarea absenta primeste 401.
- `requireCurrentUserId(requestedId)` returneaza intotdeauna ID-ul utilizatorului din sesiune. Parametrii legacy sunt optionali pe query sau pe rutele alternative `/me`; un ID legacy diferit este respins cu 403, inclusiv pentru ADMIN pe endpoint-urile personale.
- `ResourceAuthorizationService` este verificarea reutilizabila apelata la intrarea in operatiile HTTP, inainte de serviciile business. Verifica existenta resursei, relatia AccountAccess ACTIVE, rolul si apartenenta card/categorie. Nu acorda ocolire generala pentru ADMIN.
- READ permite OWNER/CO_OWNER/VIEWER; PAY permite OWNER/CO_OWNER; inchiderea contului si administrarea cardului necesita OWNER. Rolurile necunoscute sau accesul INACTIVE nu acorda drepturi.
- Categoriile sistem ACTIVE sunt lizibile/utilizabile, dar nu modificabile din endpoint-urile personale. Categoriile private sunt lizibile/modificabile/utilizabile numai de creator; cele inactive nu sunt reutilizabile.
- Platile prin IBAN pot avea un beneficiar diferit (comportament legitim). Transfer-own si exchange necesita si acces activ la contul destinatie. ID-ul sursa si categoria sunt verificate inainte de parola sau mutatii.
- `TransactionMapper` foloseste identitatea vizualizatorului pentru raspunsurile HTTP: istoricul unei plati legitime ramane vizibil ambilor participanti, dar categoria privata si ID-ul/aliasul contului contrapartei fara acces sunt eliminate. IBAN-ul si datele tranzactiei raman vizibile.
- Erorile de autorizare sunt mapate explicit in GlobalExceptionHandler la 403, iar resursele inexistente verificate de noul serviciu la 404.

### Audit endpoint -> identitate veche -> problema -> solutie

In tabel, `uid` este ID-ul clientului, iar `me` este utilizatorul real din sesiune. Rutele legacy raman disponibile cu verificare de egalitate; frontend-ul nu mai transmite userId.

| Endpoint-uri auditate | Identitate/verificare anterioara si vulnerabilitate | Solutia verificata |
| --- | --- | --- |
| GET/POST `/api/accounts`, GET `/api/accounts/paged`, GET `/api/accounts/summary/currency` | userId query considerat identitate; citire sau creare pentru B de catre A | Derivare din sesiune, userId optional doar pentru compatibilitate; mismatch 403 |
| GET `/api/accounts/{accountId}` | Acces verificat pentru userId furnizat, nu pentru principal | ID din sesiune + AccountAccess ACTIVE/READ; strain 403, inexistent 404 |
| PUT `/api/accounts/{accountId}/close` | A putea furniza identitatea owner-ului B | Sesiune + OWNER activ; regulile de business pentru sold/status raman |
| GET `/api/transactions/user` | Istoric calculat pentru userId arbitrar | ID din sesiune, lista filtrata de relatiile active existente |
| GET `/api/transactions/account/{accountId}` | Verifica accesul lui userId arbitrar | Sesiune + READ pe accountId; informatii private ale contrapartei filtrate in ambele liste |
| POST `/api/payments/initiate` | Incarca User dupa ID query; categoria era incarcata doar dupa categoryId | Principal DB + PAY activ pe sursa + drept de folosire categorie; destinatia IBAN poate fi un tert |
| POST `/api/payments/transfer-own`, `/api/payments/exchange` | User din query; verificari sursa pe colectii fara conditie ACTIVE; categorie arbitrara | Principal DB + PAY activ pe sursa + READ activ pe destinatie + categorie permisa; mismatch 403 |
| GET/POST `/api/users/{uid}/accounts/{accountId}/card` | Identitate din path | Alias `/api/users/me/accounts/{accountId}/card`; sesiune + READ pentru GET / OWNER pentru POST |
| PATCH `.../card/{cardId}/status/{status}` | Path uid arbitrar; mismatch card-cont era eroare business 400 | OWNER din sesiune + verificare cardId-accountId; mismatch 403 |
| DELETE `.../card/{cardId}/delete` | cardId din path ignorat; serviciul stergea cardul contului fara verificare de owner | OWNER din sesiune + cardId trebuie sa apartina acelui cont; apoi ciclul de viata existent CLOSED |
| GET/POST `/api/users/{uid}/categories`, GET `.../categories/paged` | User din path; lista/creare pentru alta persoana | Alias `/api/users/me/categories`; ID doar din sesiune |
| GET/PUT/DELETE `.../categories/{categoryId}` | GET nu verifica proprietarul; actualizarile puteau produce 500; identitate din path | Sesiune + categorie sistem lizibila sau categorie privata proprie; scriere doar privata proprie; 403/404 explicite |
| GET/PUT/DELETE `/api/user/{uid}/limits` | Citire/modificare/stergere dupa uid arbitrar | Alias `/api/user/me/limits`, verificarea identitatii pentru toate cele trei metode |
| GET/PUT `/api/admin/bank-limits`, DELETE `/api/admin/bank-limits/{bankLimitId}` | ADMIN din SecurityFilterChain | Regula ADMIN pastrata; ID-ul este tinta unei operatii administrative, nu identitate |
| PUT `/api/admin/users/{userId}/unlock`, POST `/api/admin/unlock-user?email=...` | ADMIN din SecurityFilterChain | Pastrat explicit; USER respins, ADMIN legitim reuseste |
| POST `/api/admin/create-shared-account`, DELETE `/api/admin/accounts/{accountId}/access?email=...` | ADMIN; users[].email si accountId desemneaza tinte administrative | Pastrate regulile existente de business si filtrul ADMIN; fara owner implicit pe endpoint-urile USER |
| POST `/api/auth/validate-individual`, `/register`, `/login`; POST logout; GET `/api/csrf` | Date de inregistrare/login, nu identitate derivata din userId | Audit: nu exista IDOR prin ID de resursa aici; fluxurile si CSRF reverificate |
| transactionId, scheduledPaymentId, exchangeRateId | Nu exista endpoint public direct pentru autorizare/executie/modificare dupa aceste ID-uri | Serviciile/joburile interne nu au fost expuse; creatia scheduled payment trece prin verificarile initiatePayment |
| tagIds din PaymentRequestDTO | Tag este resursa globala, fara owner/userId in model; nu exista API CRUD Tag | Nu necesita un ownership fictiv; nu s-au introdus noi endpoint-uri |
| email din CategoryRequestDTO / ID-uri suplimentare in body | Creatorul categoriei era stabilit de serviciu, nu din acel email | Creatorul este acum intotdeauna principalul verificat; campurile nefolosite nu acorda identitate |

---

## Rezultatele etapei 1 (istoric)

- Structura surselor backend, frontend, resurselor, configuratiilor si testelor inventariata inainte de modificari. Modificarile deja existente in working tree au fost pastrate; niciun test existent nu a fost sters.
- CookieCsrfTokenRepository si CsrfTokenRequestAttributeHandler raman active. Login-ul custom apeleaza strategia Spring pentru schimbarea ID-ului sesiunii si stergerea tokenului anterior, apoi salveaza explicit un SecurityContext nou cu HttpServletResponse real.
- Toate modulele frontend/src/api folosesc apiFetch si aceeasi valoare API_BASE_URL, cu fallback localhost:8080/api.
- apiFetch foloseste fetch nativ, impune credentials: include, obtine un token actual inaintea fiecarei mutatii si reuneste cererile CSRF simultane intr-o singura promisiune.
- data-test.sql contine limite bancare si administrator cu BCrypt. Initializarea ramane optionala (spring.sql.init.mode=never implicit in test); se activeaza explicit pentru browser.

---

## Audit Cerințe Obligatorii (Status Monolit Complet)

| Cerinta | Stare / Dovezi |
| --- | --- |
| Minimum 6-7 entitati | **Indeplinit**: 12 entitati JPA (Account, AccountAccess, BankLimit, Card, Category, ExchangeRate, Individual, ScheduledPayment, Tag, Transaction, User, UserLimit) |
| OneToOne | **Indeplinit**: User-Individual, User-UserLimit, Transaction-ScheduledPayment; mapari JPA si schema H2/MySQL |
| OneToMany / ManyToOne | **Indeplinit**: Account-AccountAccess, User-AccountAccess, Account-Card, Category-Transaction, Account-Transaction |
| ManyToMany | **Indeplinit**: Transaction-Tag prin tabela `transaction_tags` |
| CRUD complet pentru entitati | **Indeplinit**: CRUD complet categorii, cicluri de viata conturi (create/close), carduri (create/status/delete), limite (read/update/reset), conturi partajate admin |
| Spring Data JPA, service layer, business logic | **Indeplinit**: Servicii tranzactionale, calcule dobanzi/schimb valutar, verificari limite bancare si utilizator |
| Minimum doua profiles / configuratii DB | **Indeplinit**: `test` H2 in-memory, `dev` MySQL local, `prod` MySQL productie |
| JUnit 5 + Mockito, minimum 3 scenarii | **Indeplinit**: 219 teste Java (unitare si integrare MockMvc) cu 0 esecuri |
| Validare backend/frontend | **Indeplinit**: Jakarta Bean Validation pe toate DTO-urile (`@NotBlank`, `@DecimalMin`, `@Size`, `@Pattern`, `@Valid`), validare formulare React |
| Exception handling / mesaje friendly | **Indeplinit**: `GlobalExceptionHandler` trateaza uniform 400, 401, 403, 404, 405, 500 (mesaje clare, fara stacktrace expus) |
| Custom 404/500 | **Indeplinit**: `NotFoundPage` in frontend, `ErrorBoundary` React, mapare 404 backend pentru rute inexistente si resurse absente |
| Logging | **Indeplinit**: SLF4J / Logback cu configuratie in `logback-spring.xml` |
| Pagination si sorting pentru 3 entitati | **Indeplinit**: `Account`, `Transaction`, `Category` implementeaza paginare si sortare `Pageable` in Controller si Service |
| Utilizatori din DB / BCrypt / USER, ADMIN | **Indeplinit**: `CustomUserDetailsService` + `UserRepository` + `BCryptPasswordEncoder`, blocare cont la 3 incercari esuate |
| Protectie pe rol | **Indeplinit**: Ierarhie clara USER si ADMIN; accesul la `/api/admin/**` respins pentru USER cu 403 |
| CSRF / login/logout custom | **Indeplinit**: CookieCsrfTokenRepository, Double Submit Cookie, `GET /api/csrf`, sincronizare automata in `apiFetch` |
| Remember Me | **Indeplinit**: `TokenBasedRememberMeServices` (14 zile), cookie securizat `remember-me`, checkbox UI, auto-login functional |
| README complet | **Indeplinit**: README exhaustiv in root si /proiect cu diagrama ER, catalog API, arhitectura, securitate si comenzi de rulare |

---

## Ce s-a finalizat și ce rămâne de făcut înainte de Microservicii

### Cerințe Mandatare Monolit Finalizate (100%)
1. **CSRF:** Token sincronizat, protectie pe toate mutatiile, client frontend rezilient.
2. **Autorizare resursă & Identitate pe sesiune:** Eliminarea vulnerabilitatilor IDOR, verificare acces ACTIVE (OWNER/CO_OWNER/VIEWER), filtrare date private contraparti.
3. **Validare & Tratare Erori:** Bean Validation complet pe DTO-uri, handler global pentru 400, 401, 403, 404, 405, 500, `ErrorBoundary` si `NotFoundPage` in React.
4. **Remember Me:** Token-based Remember Me 14 zile, integrare Spring Security REST + UI frontend checkbox.
5. **Documentatie:** README complet cu diagrama ER, catalog REST API detaliat, specificatii de securitate si ghid de rulare.
6. **Calitate & Acoperire:** 219 teste Java trecute, 5 teste frontend trecute, 5 scenarii E2E Playwright trecute, JaCoCo > 77% (peste pragul de 70%).

### Pași Pregătitori Înainte de Începerea Efectivă a Microserviciilor
Înainte de spargerea codului în microservicii independente, trebuie stabilite următoarele decizii de design:
1. **Definirea delimitării bounded contexts:**
   - `user-service`: Gestiune utilizatori, persoane fizice, autentificare și roluri.
   - `account-service`: Gestiune conturi bancare, acces partajat, carduri și limite.
   - `transaction-service`: Gestiune plăți, transferuri, plăți programate, categorii și cursuri valutare.
2. **Arhitectura de Securitate Distribuită:**
   - Trecerea de la sesiuni HTTP/Remember Me locale la token-uri JWT semnate asimetric (RS256) sau partajate (HMAC) și autorizare stateless la nivel de Gateway / servicii de domeniu.
3. **Infrastructură Spring Cloud:**
   - Config Server pentru centralizarea configurațiilor.
   - Eureka Server pentru Service Discovery.
   - Spring Cloud Gateway pentru expunerea punctului unic de intrare și propagarea contextului de securitate.
   - OpenFeign pentru apelurile inter-servicii sincrone.
   - Resilience4j pentru toleranță la erori (CircuitBreaker / Fallbacks).
4. **Baze de date separate per serviciu:**
   - Separarea bazei de date unice în 3 scheme distincte (`user_db`, `account_db`, `transaction_db`) cu scripturi de migrare Flyway/Liquibase.
5. **Consistență Financiară și Saga Pattern:**
   - Înlocuirea tranzacțiilor locale `@Transactional` cross-tabel cu coregrafie/orchestrare Saga pentru transferuri între conturi și debitare/creditare.
6. **Containerizare & Deployment:**
   - Dockerfile pentru fiecare microserviciu și frontend, compuse într-un `docker-compose.yml` unificat.
