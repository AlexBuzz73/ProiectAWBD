## Flux schimb valutar

Documentul descrie operatiunea de schimb valutar intre conturile detinute de acelasi utilizator, in valute diferite.

### 1. Actori implicati
- utilizator autentificat (`USER`)

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`
- utilizatorul are acces la contul sursa (rol `OWNER` sau `CO_OWNER`)
- conturile implicate sunt in status `ACTIVE`
- exista rata de curs valutar disponibila in `EXCHANGE_RATES`
- sunt permise doar conversiile:
  - `RON -> USD` si `USD -> RON`
  - `RON -> EUR` si `EUR -> RON`

### 3. Flux 1: initiere schimb valutar

1. Utilizatorul acceseaza optiunea "Schimb valutar" din dashboard.
2. Aplicatia afiseaza formularul de schimb valutar.

#### Campuri formular
- valuta sursa/destinatie
- cont sursa/destinatie (selectate din lista conturilor utilizatorului)
- suma
- categorie
- descriere

#### Comportament aditional
3. Dupa selectarea valutelor:
   - lista conturilor sursa este filtrata, astfel incat sunt afisate doar conturile utilizatorului in valuta sursa
   - lista conturilor destinatie este filtrata, astfel incat sunt afisate doar conturile utilizatorului in valuta destinatie

4. Conturile sursa si destinatie **trebuie sa fie diferite**.

### 4. Validari

- contul sursa/destinatie trebuie sa fie `ACTIVE`
- utilizatorul nu trebuie sa aiba rol `VIEWER` pe contul sursa
- suma trebuie sa fie pozitiva
- categoria este obligatorie
- valuta sursa si valuta destinatie trebuie sa fie diferite
- combinatia valutara selectata trebuie sa fie una dintre cele permise de sistem
- trebuie sa existe un curs valutar de baza in `EXCHANGE_RATES`

### 5. Calcul schimb valutar

1. Aplicatia identifica rata de schimb de baza din tabela `EXCHANGE_RATES`, astfel:
   - pentru `USD -> RON`, se foloseste direct rata `USD -> RON`
   - pentru `RON -> USD`, se foloseste rata `USD -> RON`, aplicata in sens invers
   - pentru `EUR -> RON`, se foloseste direct rata `EUR -> RON`
   - pentru `RON -> EUR`, se foloseste rata `EUR -> RON`, aplicata in sens invers

2. Se calculeaza suma convertita:
   - pentru conversia catre `RON`: `amount_converted = amount * rate`
   - pentru conversia din `RON`: `amount_converted = amount / rate`

### 6. Flux 2: autorizare schimb valutar

1. Utilizatorul confirma operatiunea.
2. Aplicatia afiseaza un popup/card de autorizare.
3. Utilizatorul introduce parola de autentificare.

#### Validari
- parola trebuie sa fie corecta
- se verifica limitele tranzactionale:
  - daca exista `USER_LIMITS`, se folosesc acelea
  - altfel, se folosesc `BANK_LIMITS`
- se verifica disponibilitatea fondurilor in contul sursa

### 7. Persistenta si procesare

4. Dupa validari:
   - se creeaza un rand in `TRANSACTIONS`:
     - `initiated_by_user_id`
     - `source_account_id`
     - `destination_account_id`
     - `category_id`
     - `amount` (suma initiala din contul sursa)
     - `currency` (valuta contului sursa)
     - `exchange_rate_id` (rata de baza utilizata pentru conversie)
     - `description`
     - `is_urgent = false`
     - `is_scheduled = false`
     - `status = EXECUTED`

### 8. Executie schimb valutar

### Validari finale
- conturile sunt `ACTIVE`
- exista fonduri suficiente in contul sursa

#### Operatii
- suma este scazuta din contul sursa
- suma **convertita** este adaugata in contul destinatie

### 9. Reguli de business

- schimburile valutare sunt executate imediat
- nu exista moduri de procesare standard/urgent/programat
- utilizatorii cu rol `VIEWER` nu pot initia operatiuni
- conturile implicate trebuie sa fie in valute diferite
- sistemul permite doar conversiile `RON <-> USD` si `RON <-> EUR`
- rata cursului valutar trebuie sa existe in sistem
- o tranzactie este stocata o singura data
- disponibilitatea fondurilor este verificata la autorizare
- in `TRANSACTIONS`, coloana `exchange_rate_id` pastreaza referinta catre rata de baza utilizata la conversie, indiferent de sensul schimbului valutar

### 10. Entitati implicate

- `TRANSACTIONS`
- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `EXCHANGE_RATES`
- `USER_LIMITS`
- `BANK_LIMITS`
- `CATEGORIES`
- `USERS`

### 11. Statusuri relevante

#### TRANSACTIONS
- `EXECUTED`
- `FAILED`

### 12. Rezultat final

- utilizatorul poate efectua schimburi valutare intre propriile conturi
- conversia este realizata pe baza cursului valutar disponibil
- operatiunea este executata imediat dupa autorizare
- soldurile conturilor sunt actualizate in timp real

### 13. Job actualizare cursuri valutare

- sistemul include un job automat care ruleaza zilnic
- job-ul preia cursurile valutare din API-ul BNR (link: https://www.bnr.ro/nbrfxrates.xml)
- sunt salvate in tabela `EXCHANGE_RATES` doar ratele:
  - `USD -> RON`
  - `EUR -> RON`
- pentru fiecare zi se pastreaza un singur curs per pereche valutara