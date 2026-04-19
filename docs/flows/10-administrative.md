## Flux functionalitati administrative

Documentul descrie operatiunile disponibile pentru utilizatorii cu rol `ADMIN`.

### 1. Actori implicati
- utilizator autentificat (`ADMIN`)

### 2. Preconditii
- utilizatorul este autentificat
- utilizatorul are rol `ADMIN`
- utilizatorul are status `ACTIVE`

### 3. Dashboard ADMIN

Dashboard-ul pentru utilizatorii cu rol `ADMIN` este diferit fata de cel al utilizatorilor standard.

#### Functionalitati disponibile (mentionate si in modulele aferente)

- Deblocare utilizator
- Creare cont partajat
- Revocare acces cont partajat pentru un utilizator
- Configurare limite globale (BANK_LIMITS)

#### Observatii
- functionalitatile disponibile sunt afisate conditionat, pe baza rolului utilizatorului
- utilizatorii `ADMIN` nu au acces la functionalitatile specifice utilizatorilor standard (plati, transferuri, schimb valutar, categorii, carduri)

### 4. Flux 1: deblocare utilizator

1. ADMIN acceseaza optiunea "Deblocare utilizator" din dashboard.
2. Aplicatia afiseaza un popup/formular:
   - camp de cautare dupa email

3. ADMIN introduce email-ul utilizatorului si confirma operatiunea.

#### Validari
- utilizatorul trebuie sa existe in sistem
- utilizatorul trebuie sa aiba status `BLOCKED`

#### Persistenta
4. Sistemul actualizeaza utilizatorul:
   - `status = ACTIVE`
   - `failed_login_attempts = 0`

### 5. Flux 2: creare cont partajat (multiaccount)

1. ADMIN acceseaza optiunea "Cont partajat nou" din dashboard.
2. Aplicatia afiseaza formularul de creare cont, cu urmatoarele campuri:
   - alias cont
   - valuta (`RON`, `USD`, `EUR`)
   - lista utilizatori (selectare dupa email)
   - roluri asociate:
      - `OWNER`
      - `CO_OWNER`
      - `VIEWER`

3. ADMIN completeaza datele si confirma.

#### Validari
- maxim 2 utilizatori per cont
- fiecare utilizator trebuie sa existe in sistem
- trebuie sa existe cel putin un `OWNER`

#### Persistenta
4. Se creeaza:
   - un rand in `ACCOUNTS`
   - randuri corespunzatoare in `ACCOUNT_ACCESS`

### 6. Flux 3: configurare limite globale

1. ADMIN acceseaza optiunea "Configurare limite globale" din dashboard.
2. Aplicatia afiseaza formularul de configurare.

#### Afisare formular
- campurile sunt preincarcate cu valorile existente din `BANK_LIMITS` (by default va exista deja un rand in tabela)

#### Campuri formular
- `max_amount_per_transaction_ron`
- `max_daily_amount_ron`
- `max_daily_transactions_count`

3. ADMIN modifica valorile si confirma.

#### Validari
- toate campurile sunt obligatorii
- valorile trebuie sa fie pozitive
- `max_daily_transactions_count` trebuie sa fie numar intreg

#### Persistenta
4. Sistemul actualizeaza setul activ de limite globale din `BANK_LIMITS`

#### Observatii
- sistemul functioneaza cu un singur set activ de limite globale

### 7. Flux 4: revocare acces cont partajat

1. ADMIN acceseaza functionalitatea de gestionare conturi partajate.
2. Selecteaza un cont multiaccount.
3. Aplicatia afiseaza lista utilizatorilor asociati contului.
4. ADMIN selecteaza utilizatorul pentru care doreste revocarea accesului si confirma operatiunea.

#### Validari
- contul trebuie sa fie de tip multiaccount
- utilizatorul trebuie sa existe in lista de acces
- nu se poate elimina ultimul utilizator cu rol `OWNER`

#### Persistenta
5. Sistemul dezactiveaza randul corespunzator din `ACCOUNT_ACCESS`:
   - `status = INACTIVE`

#### Rezultat
- utilizatorul eliminat nu mai vede contul in dashboard
- contul ramane activ pentru ceilalti utilizatori

### 8. Reguli de business

- doar utilizatorii cu rol `ADMIN` pot accesa aceste functionalitati
- utilizatorii `ADMIN` nu pot efectua operatiuni financiare (plati, transferuri, schimburi valutare)
- deblocarea unui utilizator presupune resetarea statusului si a contorului de incercari esuate
- conturile partajate pot fi create doar de `ADMIN`
- limitele globale sunt aplicabile tuturor utilizatorilor din sistem

### 9. Entitati implicate

- `USERS`
- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `BANK_LIMITS`

### 10. Statusuri relevante

#### USERS
- `ACTIVE`
- `BLOCKED`
- `CLOSED`

#### ACCOUNTS
- `ACTIVE`
- `CLOSED`

### 11. Rezultat final

- utilizatorul `ADMIN` poate gestiona utilizatorii blocati
- utilizatorul `ADMIN` poate crea conturi partajate
- utilizatorul `ADMIN` poate configura limitele globale ale sistemului
- functionalitatile administrative sunt separate de cele operationale