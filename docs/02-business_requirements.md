## 1. User Management

Aplicatia permite inregistrarea unui individ in sistem, prin introducerea datelor personale precum nume, prenume, CNP si numar de telefon. Pe baza acestor date, se creeaza un utilizator asociat, folosind o adresa de email si o parola. Utilizatorul se poate autentifica, ulterior, in aplicatie folosind aceste credentiale, iar in cazul introducerii repetate a unor credentiale incorecte, utilizatorul poate fi blocat temporar sau permanent.

## 2. Account Management

Dupa autentificare, utilizatorul poate crea unul sau mai multe conturi bancare. Fiecare cont este asociat unei valute si contine informatii precum IBAN si sold. Aplicatia permite si gestionarea conturilor partajate, unde mai multi utilizatori pot avea acces la acelasi cont.

Pentru fiecare relatie utilizator-cont, este definit un rol:
- OWNER
- CO_OWNER
- VIEWER

Utilizatorii cu rol VIEWER pot vizualiza informatiile contului, dar nu pot autoriza tranzactiile.

## 3. Card Management

Pentru fiecare cont bancar, utilizatorul poate solicita emiterea unuia sau mai multor carduri, ele fiind asociate unui cont si au informatii precum tipul cardului si statusul acestuia.

## 4. Transactions

Utilizatorii pot initia diferite tipuri de tranzactii:
- transferuri intre conturi proprii
- plati catre alte conturi
- schimburi valutare

Fiecare tranzactie contine informatii despre contul sursa, contul destinatie, suma, valuta si descriere. Tranzactiile trec printr-un flux de procesare bazat pe statusuri:
- DRAFT
- PENDING_AUTH
- AUTHORIZED
- PENDING_EXECUTION
- EXECUTED
- FAILED
- CANCELLED

## 5. Transaction Processing Logic

Aplicatia diferentiaza intre doua tipuri de procesare a tranzactiilor:
- Tranzactii standard
   - Sunt procesate dupa un anumit interval de timp, prin intermediul unui job automat. Pentru simplitate, timpul configurat nu va depasi 5 minute.
- Tranzactii urgente
   - Sunt executate imediat dupa autorizare. Acest tip de procesare este determinat prin atributul `is_urgent` la nivelul tabelei de tranzactii.

## 6. Transaction Limits

Aplicatia pune la dispozitie doua niveluri de limite tranzactionale:

1. BANK_LIMITS: reprezinta limitele globale impuse de sistem, configurabile de catre un utilizator ADMIN.
2. USER_LIMITS: reprezinta limitele configurabile de catre fiecare utilizator standard.

Reguli:
- valorile din USER_LIMITS nu pot depasi valorile din BANK_LIMITS;
- daca un utilizator nu are limite configurate, se aplica implicit limitele globale, impuse de sistem.

(Pentru validare, toate valorile tranzactiilor sunt convertite intr-o moneda de referinta, de exemplu RON)

## 7. Currency Exchange

Aplicatia permite realizarea de schimburi valutare intre conturile utilizatorului. Schimbul valutar utilizeaza rate salvate in sistem, preluate periodic dintr-un API extern, pus la dispozitie de BNR. (link: https://www.bnr.ro/nbrfxrates.xml)

## 8. Scheduled Payments

Utilizatorii pot programa tranzactii pentru a fi executate la o data viitoare. O plata programata este salvata atat in tabela de tranzactii, cat si intr-o tabela dedicata pentru gestionarea programarii. In plus, un job automat verifica zilnic platile programate si executa tranzactiile ajunse la scadenta.

## 9. Status Management

Entitatile principale din sistem utilizeaza statusuri pentru a reflecta starea curenta.

Exemple relevante:
- USERS si ACCOUNTS: ACTIVE, BLOCKED, CLOSED
- CARDS: ACTIVE, BLOCKED, EXPIRED
- TRANSACTIONS: DRAFT, AUTHORIZED, PENDING_EXECUTION, EXECUTED, FAILED
- SCHEDULED_PAYMENTS: ACTIVE, EXECUTED, FAILED