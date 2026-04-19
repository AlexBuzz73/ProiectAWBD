## Flux gestionare limite tranzactionale

Documentul descrie operatiunile de configurare si gestionare a limitelor tranzactionale, atat la nivel de utilizator (`USER_LIMITS`), cat si la nivel global (`BANK_LIMITS`).

### 1. Actori implicati
- utilizator autentificat (rol `USER`)
- utilizator cu rol `ADMIN`

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`
- by default, exista un set activ de limite globale in `BANK_LIMITS`

### 3. Flux 1: configurare limite utilizator

1. Utilizatorul acceseaza optiunea "Modificare limite" din dashboard.
2. Aplicatia redirectioneaza utilizatorul catre formularul de configurare limite.

#### Afisare formular
3. Aplicatia verifica **daca exista deja** un set de limite pentru utilizator:
   - daca da, campurile sunt preincarcate
   - daca nu, campurile sunt goale

4. Formularul contine campuri pentru urmatoarele date:
   - `max_amount_per_transaction_ron`
   - `max_daily_amount_ron`
   - `max_daily_transactions_count`

5. Utilizatorul completeaza/modifica valorile si confirma.

#### Validari
- toate campurile sunt obligatorii
- toate valorile trebuie sa fie pozitive
- `max_daily_transactions_count` trebuie sa fie numar intreg
- valorile nu pot depasi limitele globale din `BANK_LIMITS`

#### Persistenta
- Daca exista deja o inregistrare in `USER_LIMITS`, aceasta este doar actualizata
- Daca nu exista, se creeaza o noua inregistrare in `USER_LIMITS` asociata utilizatorului

### 4. Flux 2: configurare limite globale (BANK_LIMITS)

1. Utilizatorul cu rol `ADMIN` acceseaza optiunea de configurare limite globale din dashboard-ul sau.
2. Aplicatia afiseaza formularul de configurare.

#### Afisare formular
3. Campurile sunt preincarcate cu valorile existente din `BANK_LIMITS`.

4. Formularul contine aceleasi campuri:
   - `max_amount_per_transaction_ron`
   - `max_daily_amount_ron`
   - `max_daily_transactions_count`

5. ADMIN modifica valorile si confirma.

#### Validari
- toate campurile sunt obligatorii
- toate valorile trebuie sa fie pozitive
- `max_daily_transactions_count` trebuie sa fie numar intreg

#### Persistenta
6. Sistemul actualizeaza setul activ de limite globale din `BANK_LIMITS` (by default, va exista deja un set de limite impuse de banca)

#### Observatii
- sistemul functioneaza cu un singur set activ de limite globale

### 5. Reguli de business

- valorile din `USER_LIMITS` nu pot depasi valorile din `BANK_LIMITS`
- daca un utilizator nu are limite proprii se aplica automat limitele globale in tranzactii
- toate valorile sunt exprimate in RON
- limitele sunt utilizate in validarea tranzactiilor
- salvarea este permisa chiar daca valorile nu sunt modificate

### 6. Entitati implicate

- `USER_LIMITS`
- `BANK_LIMITS`
- `USERS`

### 7. Statusuri relevante

#### USER_LIMITS
- `ACTIVE`
- `INACTIVE`

#### BANK_LIMITS
- `ACTIVE`

### 8. Rezultat final

- utilizatorul isi poate configura propriile limite tranzactionale
- administratorul poate configura limitele globale ale sistemului
- limitele sunt aplicate ulterior in procesarea tranzactiilor