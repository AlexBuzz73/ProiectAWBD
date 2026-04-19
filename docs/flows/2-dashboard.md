## Flux accesare dashboard si vizualizare date

Documentul descrie comportamentul aplicatiei dupa autentificarea cu succes a utilizatorului, precum si informatiile afisate in ecranul principal pentru utilizatorii cu rol `USER`.

### 1. Actori implicati
- utilizator autentificat (rol `USER`)

### 2. Preconditii
- utilizatorul este autentificat cu succes, are status `ACTIVE` si are rol `USER`

### 3. Flux: accesare dashboard

1. Dupa autentificare reusita, utilizatorul este redirectionat automat catre dashboard.
2. Aplicatia incarca toate conturile la care utilizatorul are acces, pe baza tabelei `ACCOUNT_ACCESS`.

#### Afisare conturi

3. Conturile sunt afisate sub forma de carduri (componenta UI), fiecare continand:
   - alias cont
   - IBAN
   - sold curent

4. Utilizatorul poate selecta un cont pentru a vizualiza detalii.

#### Afisare tranzactii recente

5. Aplicatia afiseaza ultimele 5 tranzactii ale utilizatorului, indiferent de cont

#### Navigare

6. Dashboard-ul contine optiuni de navigare catre fluxurile principale:

- Creare cont bancar nou (single account)
- Initiere plata (plati interne/externe)
- Transfer intre conturi proprii
- Schimb valutar
- Vizualizare si gestionare categorii
- Modificare limite utilizator (USER_LIMITS)

### 4. Diferentiere roluri

- Dashboard-ul este afisat diferit in functie de rolul utilizatorului:
  - utilizatorii cu rol `USER` vad functionalitatile operationale (plati, conturi, schimb valutar etc.)
  - utilizatorii cu rol `ADMIN` au un dashboard separat, cu functionalitati administrative (mentionate in documentul aferent)

- utilizatorii `USER` nu au acces la:
  - deblocare utilizatori
  - creare conturi partajate
  - configurare limite globale
  - revocare acces cont partajat

### 5. Reguli de business

- sunt afisate toate conturile la care utilizatorul are acces, indiferent de rol (`OWNER`, `CO_OWNER`, `VIEWER`)
- tranzactiile afisate sunt globale la nivel de utilizator
- doar utilizatorii autentificati pot accesa dashboard-ul
- optiunile disponibile sunt conditionate de rolul utilizatorului

### 6. Entitati implicate

- `USERS`
- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `TRANSACTIONS`

### 7. Statusuri relevante

#### USERS
- `ACTIVE`

#### ACCOUNTS
- `ACTIVE`
- `CLOSED`

### 8. Rezultat final

- utilizatorul vizualizeaza conturile proprii si partajate
- utilizatorul vizualizeaza tranzactiile recente
- utilizatorul poate naviga catre toate fluxurile operationale ale aplicatiei
- accesul la functionalitati este limitat in functie de rol