# Frontend Internet Banking

React + Vite; autentificare cu sesiune Spring Security si cookie CSRF. Backend-ul este in directorul parinte.

## Rulare locala

Necesita JDK 25 pentru proiectul Gradle si Node.js compatibil cu Vite instalat (verificat cu Node 24.15.0).
Din root, cu JAVA_HOME indicand JDK 25:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=test"
```

H2 este in memorie si porneste gol. Pentru demonstrarea platilor si operatiilor ADMIN, opreste instanta anterioara si porneste cu datele optionale din data-test.sql:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=test --spring.sql.init.mode=always"
```

Aceasta varianta creeaza limite bancare si administratorul local `admin@test.com` / `TestAdmin123!` cu parola stocata BCrypt. Datele se pierd la oprire. Datele si credentialele sunt exclusiv pentru H2 local.

Intr-un al doilea terminal:

```powershell
cd frontend
npm install
npm run dev -- --host localhost --strictPort
```

Deschide http://localhost:5173. API-ul implicit este http://localhost:8080/api.
Optional, seteaza `VITE_API_BASE_URL` in `.env` dupa modelul `.env.example`.
Foloseste `localhost` pentru ambele aplicatii, conform configuratiei CORS existente.

## Verificari

```powershell
npm run lint
npm test
npm run build
npm run test:e2e
```

Testele E2E necesita backend-ul cu datele optionale si Vite pornite pe porturile de mai sus. Folosesc Microsoft Edge headless instalat local (`channel: msedge`). Pe alte sisteme se poate instala Edge cu `npx playwright install msedge` sau configura un browser Playwright disponibil.

E2E creeaza utilizatori si date in H2 prin API-uri reale. Register/login/logout sunt exercitate prin UI; celelalte mutatii folosesc modulele Vite API in contextul browserului, cu cookie-uri si CORS reale. Testele pot fi repetate datorita identificatorilor unici. Nu sunt destinate unei baze de productie.

Clientul comun `src/api/apiClient.js` obtine CSRF prin fetch nativ, include credentialele si trimite X-XSRF-TOKEN pentru mutatii. Cererile bootstrap simultane sunt reunite; nu exista retry automat al unei plati respinse.

Din root:

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean test jacocoTestReport build
```

Rezultatele si problemele ramase sunt in [PROJECT_PROGRESS.md](../PROJECT_PROGRESS.md). Autorizarea intre utilizatori este verificata prin doua sesiuni reale. Modulele API nu mai accepta userId; backend-ul deriva identitatea din sesiune, cu rute /users/me/... si /user/me/limits. Tratarea generala API 404 si alte cerinte din checklist raman deschise; aplicatia nu este declarata pregatita pentru productie.
