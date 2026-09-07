# PROJECT_PROGRESS

Actualizat: 2026-09-07. Etapa curenta: Faza 1 – Extragerea user-service finalizata cu succes. Monolitul ramane 100% stabil si functional (tag git: `monolith-stable`).
user-service este un microserviciu Spring Boot complet independent, cu propria baza de date, build propriu, teste trecute si autentificare JWT (RS256).

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
- [ ] account-service
- [ ] transaction-service
- [ ] Eureka
- [ ] OpenFeign
- [ ] Gateway
- [ ] Load balancing
- [ ] JWT/distributed security (in progres: emitere si validare Bearer JWT in user-service; validare la nivel de Gateway/Resursa distribuit pe celelalte servicii urmeaza)
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

## Faza 1 – Extragere user-service (finalizată)

Data finalizarii: 2026-09-07. S-a creat primul microserviciu de business independent: **`user-service`** (port 8081).
Monolitul preexistent este stabilizat si pastrat intact sub tag-ul git **`monolith-stable`** (219 teste Java, suita E2E, CSRF si Remember Me).

### 1. Granița Arhitecturală și Responsabilități

`user-service` este o aplicatie Spring Boot 4 / Java 25 complet de sine statatoare, localizata in `user-service/` (cu link de acces in radacina repository-ului).
- **Responsabilități:**
  - Gestionare `User` si `Individual` (profil persoana fizica)
  - Validare date de inregistrare (CNP unic, varsta minima 18 ani)
  - Hashing parole cu `BCryptPasswordEncoder`
  - Autentificare credetiale si emitere de token-uri JWT asimetrice (RS256)
  - Contorizare incercari esuate de login si blocare automata a contului dupa 3 esecuri
  - Deblocare conturi de catre `ADMIN` (prin ID sau email)
  - Expunere profil utilizator curent (`GET /api/users/me`)
  - Endpoint-uri de lookup sigure pentru comunicare inter-servicii viitoare (`GET /api/users/{id}`, `GET /api/users/by-email`)

### 2. Izolarea Datelor (Database Ownership)

- `user-service` detine in mod exclusiv schema **`user_db`** (tabelele `users` si `individuals`).
- Niciun alt serviciu viitor (`account-service`, `transaction-service`) nu are acces SQL direct la `user_db`.
- Entitatile JPA din `user-service` nu sunt partajate cu alte servicii si nu contin referinte catre conturi sau tranzactii.
- Profiluri de baza de date:
  - `test`: H2 in-memory (`jdbc:h2:mem:user_db`, `ddl-auto=create-drop`).
  - `dev`: MySQL local (`jdbc:mysql://localhost:3306/user_db`, `ddl-auto=update`).
  - `prod`: Configurare parametrizata din variabile de mediu (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), fara secrete in cod.

### 3. Design de Securitate Distribuită & JWT

- **Standard Spring Security:** Implementat cu `spring-boot-starter-oauth2-resource-server` si `spring-security-oauth2-jose` (Nimbus JOSE).
- **Semnare Asimetrică (RS256):** Cheie RSA pe 2048 de biți generata in `JwtConfig` cu posibilitate de injectare a cheilor externe.
- **Claims JWT:**
  - `iss`: `"user-service"`
  - `sub`: email-ul utilizatorului
  - `userId`: ID-ul numeric al utilizatorului
  - `username`: numele de utilizator
  - `role`: `"USER"` sau `"ADMIN"`
  - `iat` / `exp`: durata de valabilitate de 2 ore (7200 secunde).
  - **Filtrare strictă:** Niciun camp sensibil (parola, hash, CNP, telefon) nu este inclus in claims.
- **Autentificare Stateless Bearer & CSRF:**
  - Autentificarea foloseste antetul HTTP `Authorization: Bearer <token>`.
  - CSRF este dezactivat in `user-service` (`csrf.disable()`) intrucat browserele nu trimit automat antetul `Authorization` in cereri cross-origin (spre deosebire de cookie-urile `JSESSIONID` din monolit, unde CSRF ramane activ).

### 4. API Implementat (Port 8081)

- `POST /api/auth/validate-individual` -> 200 / 400
- `POST /api/auth/register` -> 201 / 400
- `POST /api/auth/login` -> 200 (returneaza JWT, tokenType Bearer, expiresIn 7200, rol) / 400
- `GET /api/users/me` -> 200 (profil `UserResponseDTO` fara hash/CNP) / 401
- `GET /api/users/{id}` -> 200 (`UserLookupDTO` sigur) / 401 / 404
- `GET /api/users/by-email?email=...` -> 200 (`UserLookupDTO`) / 401 / 404
- `PUT /api/admin/users/{userId}/unlock` -> 200 / 401 / 403 (doar ADMIN)
- `POST /api/admin/unlock-user?email=...` -> 200 / 401 / 403 (doar ADMIN)

### 5. Verificare și Teste Executate

#### Teste `user-service` (15 metode de test acoperind cele 17 cerințe obligatorii):
1. Inregistrare valida -> creare utilizator si individ in DB, parola BCrypt.
2. Inregistrare email duplicat -> 400.
3. Inregistrare username duplicat -> 400.
4. Date invalide (sub 18 ani, parola scurta, campuri goale) -> 400.
5. Login valid -> 200 cu JWT valid.
6. Parola invalida -> 400.
7. Blocare utilizator dupa 3 incercari esuate -> cont blocat, mesaj de contact banca.
8. Deblocare admin (dupa ID si dupa email) -> contul revine la ACTIVE si permite login.
9. Parola BCrypt verificata ca nu este salvata in clar (prefix `$2a$`, matches).
10. Login returneaza token JWT structurat (3 parti separate prin `.`).
11. Claims JWT contin `sub`, `userId`, `username`, `role` si nu contin parole/CNP/telefon.
12. Endpoint protejat fara token -> 401 Unauthorized (`{"error": "Autentificare necesara"}`).
13. Endpoint protejat cu token invalid -> 401 Unauthorized.
14. Utilizator obisnuit care acceseaza endpoint de admin -> 403 Forbidden (`{"error": "Acces interzis"}`).
15. Administrator care acceseaza endpoint protejat -> 200 OK.
16. `GET /api/users/me` returneaza datele utilizatorului curent fara campuri sensibile.
17. Endpoint protejat functioneaza cu JWT valid.
18. Validare individuala CNP si varsta.
19. Lookup inter-servicii sigur (`UserLookupDTO`) si raspuns 404 pentru utilizator inexistent.
20. Metoda HTTP nepermisa -> 405 Method Not Allowed.

**Rezultate comenzi `user-service`:**
- `.\gradlew.bat clean test`: **BUILD SUCCESSFUL, 15 teste, 0 failures, 0 skipped**.
- `.\gradlew.bat build`: **BUILD SUCCESSFUL, JAR generat**.
- Acoperire JaCoCo: **86% instrucțiuni** (1.105 / 1.278), **89% linii** (270 / 303), **100% controller-e**, **87% servicii**.
- Test live HTTP: verificat comportament real prin HTTP (401 la request neautentificat, 201 la inregistrare, 200 la login cu Bearer token emis, 200 la `/api/users/me` cu Bearer token).

#### Verificare de Regresie pe Monolit:
- `.\gradlew.bat clean test`: **BUILD SUCCESSFUL, 219 teste, 0 failures, 0 skipped**. Monolitul ramane complet functional si neafectat.

---

## Etapa 3 – Validare, Error Handling, Remember Me și Documentație (istoric)

Data verificarii: 2026-09-07. S-au implementat si verificat tratarea unitara a erorilor HTTP, validarea completa pe DTO-uri si controller-e, mecanismul Remember Me (backend + frontend), ErrorBoundary si pagina 404 in frontend, documentatia exhaustiva in README.md (cu diagrama ER Mermaid si catalog complet de API) si curatarea proprietatilor de productie.

---

## Etapa 2 – Autorizarea resurselor pe baza sesiunii (istoric)

Data verificarii: 2026-09-07. Problema IDOR a fost remediata pentru endpoint-urile HTTP. SecurityConfig, apiFetch si mecanismul CSRF din etapa 1 au fost pastrate.

---

## Rezultatele etapei 1 (istoric)

- Structura surselor backend, frontend, resurselor, configuratiilor si testelor inventariata inainte de modificari. Modificarile deja existente in working tree au fost pastrate; niciun test existent nu a fost sters.
- CookieCsrfTokenRepository si CsrfTokenRequestAttributeHandler raman active. Login-ul custom apeleaza strategia Spring pentru schimbarea ID-ului sesiunii si stergerea tokenului anterior, apoi salveaza explicit un SecurityContext nou cu HttpServletResponse real.
- Toate modulele frontend/src/api folosesc apiFetch si aceeasi valoare API_BASE_URL, cu fallback localhost:8080/api.

---

## Planul Următorilor Pași (Faza 2: account-service)

Microserviciile rămase **NU au fost începute** în această etapă:
1. **`account-service` (Faza 2 următoare):**
   - Extragere conturi bancare, acces partajat (multi-user: OWNER, CO_OWNER, VIEWER), emitere și administrare carduri, limite de tranzacționare.
   - Deținerea schemei `account_db`.
   - Validare JWT ca OAuth2 Resource Server.
   - Apeluri inter-servicii către `user-service` pentru validarea existenței utilizatorilor.
2. **`transaction-service` (Faza 3 următoare):**
   - Extragere plăți standard și urgente, transferuri proprii, plăți programate, schimb valutar, categorii.
   - Deținerea schemei `transaction_db`.
3. **Infrastructură Distribuită (Faza 4):**
   - Eureka Service Discovery, Spring Cloud Gateway, Config Server, Resilience4j, Saga Pattern.
