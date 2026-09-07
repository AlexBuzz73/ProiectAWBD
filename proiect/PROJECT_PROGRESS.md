# PROJECT_PROGRESS

Actualizat: 2026-09-07. Etapa curenta: autorizare resurse pe identitatea sesiunii, finalizata. Rezultatele etapei 1 sunt pastrate ca istoric.
Migrarea la microservicii nu a inceput. Bifele indica verificari efectuate, nu doar existenta unor fisiere.

## Checklist

- [ ] Mandatory requirements
- [x] CSRF
- [x] Resource authorization (session identity / ownership)
- [ ] Remember Me
- [ ] Error handling
- [ ] 404/500
- [x] Pagination
- [x] Coverage >=70%
- [ ] README
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

## Etapa 2 � Autorizarea resurselor pe baza sesiunii (finalizata)

Data verificarii: 2026-09-07. Problema IDOR a fost remediata pentru endpoint-urile HTTP actuale. Nu s-au inceput Remember Me sau microservicii. SecurityConfig, apiFetch si mecanismul CSRF din etapa 1 au fost pastrate.

### Design

- `CurrentUserService` citeste Authentication din SecurityContextHolder, respinge contextul anonim si incarca User prin email-ul principalului din UserRepository. Un utilizator DB absent/inactiv primeste 403; autentificarea absenta primeste 401.
- `requireCurrentUserId(requestedId)` returneaza intotdeauna ID-ul utilizatorului din sesiune. Parametrii legacy sunt optionali pe query sau pe rutele alternative `/me`; un ID legacy diferit este respins cu 403, inclusiv pentru ADMIN pe endpoint-urile personale.
- `ResourceAuthorizationService` este verificarea reutilizabila apelata la intrarea in operatiile HTTP, inainte de serviciile business. Verifica existenta resursei, relatia AccountAccess ACTIVE, rolul si apartenenta card/categorie. Nu acorda ocolire generala pentru ADMIN.
- READ permite OWNER/CO_OWNER/VIEWER; PAY permite OWNER/CO_OWNER; inchiderea contului si administrarea cardului necesita OWNER. Rolurile necunoscute sau accesul INACTIVE nu acorda drepturi.
- Categoriile sistem ACTIVE sunt lizibile/utilizabile, dar nu modificabile din endpoint-urile personale. Categoriile private sunt lizibile/modificabile/utilizabile numai de creator; cele inactive nu sunt reutilizabile.
- Platile prin IBAN pot avea un beneficiar diferit (comportament legitim). Transfer-own si exchange necesita si acces activ la contul destinatie. ID-ul sursa si categoria sunt verificate inainte de parola sau mutatii.
- `TransactionMapper` foloseste identitatea vizualizatorului pentru raspunsurile HTTP: istoricul unei plati legitime ramane vizibil ambilor participanti, dar categoria privata si ID-ul/aliasul contului contrapartei fara acces sunt eliminate. IBAN-ul si datele tranzactiei raman vizibile.
- Erorile de autorizare sunt mapate explicit in GlobalExceptionHandler la 403, iar resursele inexistente verificate de noul serviciu la 404. Tratarea generala a URL-urilor necunoscute/500 nu este finalizata.
- Joburile interne nu au fost obligate sa simuleze o sesiune HTTP. Noile endpoint-uri viitoare trebuie sa aplice aceeasi frontiera de autorizare; serviciile business nu sunt toate protejate individual cu method security.

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

### Teste noi si adaptarea celor existente

`ResourceAuthorizationIntegrationTest`: **43 de cazuri Java noi** (inclusiv parameterized tests), cu utilizatori A/B/ADMIN persistati, conturi, carduri, categorii si limite H2. Context DB separat, rollback tranzactional; filtrele Spring Security sunt active.

Acopera toate cerintele cross-user: spoof userId in dashboard, account details/close, sursa plata/transfer/exchange, destinatie straina la own-transfer/exchange, card strain si cardId strain pe cont propriu, categorii private, user limits, toate cele 7 operatii administrative refuzate USER-ului, acces revocat, VIEWER, CO_OWNER, ADMIN fara owner implicit, resurse inexistente, cont DB blocat/absent, sesiune reala de login si operatii proprii reusite. Verifica si lipsa mutatiilor dupa refuz, plus eliminarea etichetelor private din istoricul tranzactiilor.

Cele 158 de teste anterioare sunt pastrate. Account/Payment folosesc acum email-ul utilizatorului DB ca principal in loc de un nume arbitrar. Card/Category/Limit au acum filtre active, CSRF si principal corect; scenariile de autorizare asteapta 403 in loc de 400. Curatarea fixture-urilor foloseste operatii batch in ordinea relatiilor, evitand cascada User-Individual la partajarea contextului H2. Testele business raman prezente.

Frontend: nou `currentUserApis.test.js` verifica rutele /me, absenta userId pe wire, credentials si CSRF; nou `authorization.spec.js` verifica doua sesiuni reale in browser. A nu poate ataca B prin 13 request-uri, nici schimband localStorage; B isi poate reciti resursele nemodificate. Cele 3 scenarii E2E anterioare au fost pastrate si migrate la semnaturile API fara userId.

### Rezultate finale

| Comanda/verificare | Rezultat |
| --- | --- |
| `.\gradlew.bat clean test` | BUILD SUCCESSFUL, 201 teste, zero failures/errors/skipped |
| `.\gradlew.bat clean test jacocoTestReport build` | BUILD SUCCESSFUL, 201 teste; JAR si rapoarte generate |
| `npm run lint` | PASS, 0 erori/avertismente |
| `npm test` | PASS, 3 teste |
| `npm run build` | PASS |
| `npm run test:e2e` | PASS, 4 scenarii Microsoft Edge headless, inclusiv 2 sesiuni independente |
| CSRF | Scenariile existente de register/login/logout, refresh, plati si respingere token invalid/lipsa trec |

Coverage final din `build/reports/jacoco/test/jacocoTestReport.xml`:

| Domeniu | Instructiuni | Linii |
| --- | --- | --- |
| services.impl | **78.68%** (3339/4244) | **81.40%** (674/828) |
| services + services.impl, inclusiv autorizare si joburi | **75.14%** (3634/4836) | **76.42%** (729/954) |

Pragul agregat >=70% este indeplinit fara excluderea serviciilor noi sau a joburilor. Rapoarte: `build/reports/tests/test/index.html`, `build/reports/jacoco/test/html/index.html`.

### Fisiere modificate in etapa 2

- Noi: `services/CurrentUserService.java`, `services/ResourceAuthorizationService.java`, `exceptions/ResourceNotFoundException.java` (sub src/main/java/com/example/demo).
- Backend existent: controller-ele Account, Transaction, Payment, Card, Category, Limit; `exceptions/GlobalExceptionHandler.java`; `mappers/TransactionMapper.java`; `services/impl/TransactionServiceImpl.java`.
- Java tests: nou `integration/ResourceAuthorizationIntegrationTest.java`; adaptate AccountIntegrationTest, PaymentFlowsIntegrationTest, CardControllerIntegrationTest, CategoryControllerIntegrationTest, LimitControllerIntegrationTest.
- API frontend: accountsApi.js, transactionsApi.js, paymentsApi.js, cardsApi.js, categoriesApi.js, limitsApi.js. apiFetch, authApi si modulele ADMIN sunt pastrate.
- Apelanti frontend: components/cards/AccountCardSection.jsx; pages/user/AccountDetailsPage.jsx, CategoriesPage.jsx, ExchangePage.jsx, NewPaymentPage.jsx, SingleAccountCreatePage.jsx, TransferPage.jsx, UserDashboardPage.jsx, UserLimitsPage.jsx.
- Teste frontend: e2e/csrf.spec.js (adaptat); noi e2e/authorization.spec.js si tests/currentUserApis.test.js.
- Documentatie: PROJECT_PROGRESS.md, frontend/README.md. Nu s-au modificat build.gradle, configuratia de securitate sau datele de productie in aceasta etapa.

### Limite si etapa urmatoare

Problema IDOR documentata este rezolvata pentru rutele auditate, cu acces legitim la conturi partajate si plati catre terti pastrat. Rutele legacy pot fi eliminate intr-o etapa ulterioara; acum sunt verificate, nu ignorate. Codul de autorizare este aplicat la frontiera HTTP; orice nou punct de intrare trebuie sa-l foloseasca.

Raman deschise: tratarea generala 404/500, validarea completa DTO/frontend, Remember Me, README complet cu ER/API/deployment, configuratia MySQL reproductibila, TLS BNR si aspectele de consistenta financiara mentionate mai jos. Urmatoarea etapa recomandata: error handling si validare (inclusiv URL API inexistent -> 404), apoi Remember Me. Nu se trece inca la microservicii.


## Rezultatele etapei 1 (istoric)

- Structura surselor backend, frontend, resurselor, configuratiilor si testelor inventariata inainte de modificari. Modificarile deja existente in working tree au fost pastrate; niciun test existent nu a fost sters.
- Procesul vechi Java PID 10396 apartinea acestui repository si raspundea 401 la GET /api/csrf. A fost oprit; codul curent pornit cu profilul test raspunde 200 anonim, cu token si cookie XSRF-TOKEN.
- CookieCsrfTokenRepository si CsrfTokenRequestAttributeHandler raman active. Login-ul custom apeleaza strategia Spring pentru schimbarea ID-ului sesiunii si stergerea tokenului anterior, apoi salveaza explicit un SecurityContext nou cu HttpServletResponse real.
- Toate modulele frontend/src/api folosesc apiFetch si aceeasi valoare API_BASE_URL, cu fallback localhost:8080/api. Corectat apiapiFetch din transactionsApi.js.
- apiFetch foloseste fetch nativ, impune credentials: include, obtine un token actual inaintea fiecarei mutatii si reuneste cererile CSRF simultane intr-o singura promisiune. Nu reexecuta automat platile la 403. Costul acestei solutii simple: un GET suplimentar pentru fiecare mutatie secventiala.
- Logout-ul verifica raspunsul serverului; toate cele trei butoane de logout afiseaza erorile si pastreaza starea locala daca deconectarea nu reuseste.
- Corectate problemele semnalate de ESLint in efectele React, initializarea formularelor de limite/plati si importuri. Regulile ESLint nu au fost dezactivate.
- data-test.sql, incompatibil cu schema curenta, a fost inlocuit cu date H2 minimale: limite bancare si administrator cu BCrypt. Initializarea ramane optionala (spring.sql.init.mode=never implicit in test); se activeaza explicit pentru browser. Nu s-au schimbat date MySQL.

## Validare executata in etapa 1 (istoric)

| Verificare | Rezultat |
| --- | --- |
| gradlew.bat clean test, inainte de modificari | PASS, 147 teste existente |
| gradlew.bat test --tests '*CsrfIntegrationTest' --tests '*AuthIntegrationTest' | PASS dupa izolarea contextului testelor cu cookie real |
| gradlew.bat clean test, dupa modificari | PASS, 158 teste, 0 esecuri, 0 erori, 0 ignorate |
| gradlew.bat clean test jacocoTestReport build | BUILD SUCCESSFUL, 158 teste; rapoarte JaCoCo si JAR executabil generate |
| npm install | PASS; audit: 0 vulnerabilitati raportate |
| npm run dev -- --host localhost --strictPort | Vite pornit pe 5173 |
| npm run lint | PASS, 0 erori / avertismente |
| npm test | PASS, 2 teste pentru clientul API |
| npm run build | PASS |
| npm run test:e2e | PASS, 3 scenarii in Microsoft Edge headless, backend real H2 |

Testele noi MockMvc verifica tokenul anonim, cookie/header reale, CORS, preflight, POST/PUT/PATCH/DELETE fara token sau cu token invalid, register/login, schimbarea ID-ului sesiunii, refresh CSRF, creare cont, CRUD categorii, interdictia USER -> ADMIN, logout si relogare.
Contextul lor este separat de testele existente cu .with(csrf()), deoarece acel helper inlocuieste repository-ul CSRF al filtrului MockMvc. Nu s-a relaxat configuratia de productie pentru teste.

Scenarii browser:

1. Formulare register si login; modulele API reale pentru limite personale, creare conturi, card/PATCH, categorie/PUT/DELETE, transfer cu debitare, plata urgenta executata, inchidere cont, paginare conturi/tranzactii, logout si relogare, pagina categorii.
2. Login ADMIN, citire/actualizare limite, deblocare utilizator, creare cont comun, revocare acces, formular limite globale, logout.
3. 401 pentru acces anonim protejat; 403 pentru token lipsa/invalid; trei cereri concurente folosesc un singur bootstrap CSRF si toate includ credentials, inclusiv cand apelantul specifica omit.

Scenariile pozitive nu au produs erori JavaScript sau raspunsuri 401/403 neasteptate. Testul negativ produce deliberat 401/403/400. Browserul foloseste modulele API reale pentru mutatii; nu toate formularele au fost completate manual/automat prin UI.

## Coverage masurat in etapa 1 (istoric)

Sursa: build/reports/jacoco/test/jacocoTestReport.xml, dupa suita completa.

| Domeniu | Instructiuni | Linii |
| --- | --- | --- |
| services.impl | 74.45% (3155/4238) | 75.72% (627/828) |
| services + services.impl, inclusiv joburi | 70.29% (3243/4614) | 70.51% (648/919) |
| Aplicatia intreaga | 73.11% | 74.00% |

Branch coverage services.impl: 58.52%. Joburile au coverage mai mic; pragul de 70% este indeplinit agregat, nu pentru fiecare clasa si nu pentru ramuri. Nu exista inca un prag JaCoCo obligatoriu in task-ul check.

## Audit cerinte obligatorii

| Cerinta | Stare / dovezi |
| --- | --- |
| Minimum 6-7 entitati | Indeplinit: 12 entitati JPA (Account, AccountAccess, BankLimit, Card, Category, ExchangeRate, Individual, ScheduledPayment, Tag, Transaction, User, UserLimit) |
| OneToOne | User-Individual, User-UserLimit, Transaction-ScheduledPayment; schema H2 creata de teste |
| OneToMany / ManyToOne | Account-AccountAccess, User-AccountAccess si alte relatii; scenarii executate |
| ManyToMany | Transaction-Tag prin transaction_tags; mapping si schema existente, flux de administrare taguri incomplet |
| CRUD complet pentru entitati | Partial: CRUD categorii, cicluri de viata conturi/carduri/limite; lipsesc API-uri complete pentru users/individuals/tags si administrarea unor resurse. Nu sunt recomandate stergeri arbitrare de tranzactii financiare; cerinta trebuie mapata explicit pe entitati eligibile |
| Spring Data JPA, service layer, business logic | Existente; teste unitare si integrare trecute |
| Minimum doua profiles / configuratii DB | dev/prod MySQL, test H2; rularea H2 verificata. MySQL dev/prod nu a fost pornit/verificat in aceasta etapa |
| JUnit 5 + Mockito, minimum 3 scenarii | Indeplinit; actualizat in etapa 2 la 201 teste Java plus 4 scenarii browser |
| Validare backend/frontend | Exista, dar incompleta: CategoryRequestDTO nu are constrangeri Bean Validation; SharedAccountRequest nu este validat cu @Valid la controller; este necesar audit pe toate DTO-urile |
| Exception handling / mesaje friendly | Partial: handler 400 business/validare si mesaj generic 500 exista; erorile nu au format uniform si unele erori de client devin 500 |
| Custom 404/500 | Pagina React NotFoundPage exista si handler generic 500 exista; GET autentificat /api/does-not-exist a returnat 500, trebuie corectat in 404 |
| Logging | SLF4J/Logback, logback-spring.xml si loguri de rulare verificate; revizuire niveluri/date personale necesara inainte de deployment |
| Pagination si sorting pentru 3 entitati | Indeplinit pentru accounts, transactions, categories; teste servicii trecute si citiri paginated prin browser |
| Utilizatori din DB / BCrypt / USER, ADMIN | Indeplinit prin CustomUserDetailsService + UserRepository + DaoAuthenticationProvider; nu utilizeaza schema standard JdbcUserDetailsManager |
| Protectie pe rol | ADMIN protejat fata de USER; IDOR remediat si ownership verificat in etapa 2, inclusiv conturi partajate si categorii private |
| CSRF / login/logout custom | Indeplinit pentru scenariile acestei etape |
| Remember Me | Lipseste configurarea backend si optiunea UI; localStorage loggedUser nu reprezinta Remember Me |
| README complet | Lipseste README in root cu arhitectura, ER, API, environment/DB, security, coverage si deployment. frontend/README.md documenteaza doar rularea/verificarea acestei etape |

## Probleme deschise si urmatorul pas

1. **IDOR rezolvat in etapa 2:** identitate din sesiune, ownership, 43 teste Java noi si un scenariu browser cu doua sesiuni. Vezi auditul si rezultatele curente de mai sus.
2. Corectarea statusurilor 404/400/403 si completarea validarii; apoi Remember Me si documentatia obligatorie.
3. BNR: jobul de startup raporteaza PKIX path building failed in acest mediu. Aplicatia porneste, insa cursurile nu sunt incarcate automat. Verificarea TLS nu a fost dezactivata. Schimbul valutar este acoperit de testele existente cu date H2, nu de o verificare live BNR reusita.
4. Configuratiile MySQL folosesc ddl-auto=validate fara migrari de schema incluse; sunt necesare migrari/reproducerea DB. application-prod.properties contine referinta preexistenta la data-test.sql (init implicit never): trebuie eliminata in etapa de configurare productie.
5. Inaintea distribuirii transferurilor: sumele sunt double, nu exista @Version/locking pe Account, checkLimits verifica doar limita pe tranzactie. Sunt necesare analiza concurentei, idempotenta si limite zilnice verificate in teste.
6. H2 console este permitAll, iar frameOptions este dezactivat global in configuratia existenta. Restrictionare pe profil si revizuire headere in etapa de securitate.
7. Exista doar Docker Compose pentru MySQL, fara Dockerfile aplicatie/frontend sau deployment. Componentele distribuite raman nebifate.

Nu s-au inceput microservicii. Urmatorul pas recomandat este completarea error handling si validarii, inainte de Remember Me sau extragerea serviciilor.

## Fisiere modificate in etapa 1 (istoric)

Modificarile enumerate sunt suplimentare fata de cele deja prezente la inceput:

- Backend: src/main/java/com/example/demo/config/SecurityConfig.java; src/main/java/com/example/demo/controllers/AuthController.java; src/main/resources/data-test.sql; nou src/test/java/com/example/demo/integration/CsrfIntegrationTest.java. CsrfController existent verificat si pastrat.
- API frontend: toate cele 10 fisiere din frontend/src/api (apiClient, accountsApi, adminApi, authApi, cardsApi, categoriesApi, limitApi, limitsApi, paymentsApi, transactionsApi).
- Componente frontend: App.jsx; components/common/Navbar.jsx; components/cards/AccountCardSection.jsx; components/limits/BankLimitsForm.jsx si UserLimitsForm.jsx; components/payments/PaymentForm.jsx si TransferForm.jsx.
- Pagini frontend: LoginPage.jsx; BankLimitsPage.jsx; admin/AdminDashboardPage.jsx; user/AccountDetailsPage.jsx, CategoriesPage.jsx, UserDashboardPage.jsx, UserLimitsPage.jsx.
- Testare/documentatie: frontend/package.json si package-lock.json; frontend/playwright.config.js; frontend/e2e/csrf.spec.js; frontend/tests/apiClient.test.js; frontend/README.md; .gitignore; PROJECT_PROGRESS.md.
- build.gradle si testele vechi aveau deja modificari: au fost pastrate, nu reconstruite si nu sterse.
