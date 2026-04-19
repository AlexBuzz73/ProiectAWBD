## Flux transfer intre conturile proprii

Documentul descrie operatiunea de transfer de fonduri intre conturile detinute de acelasi utilizator.

### 1. Actori implicati
- utilizator autentificat (`USER`)

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`
- utilizatorul are acces la contul sursa (rol `OWNER` sau `CO_OWNER`)
- conturile implicate sunt in status `ACTIVE`

### 3. Flux 1: initiere transfer

1. Utilizatorul acceseaza optiunea "Transfer intre conturi" din dashboard.
2. Aplicatia afiseaza formularul de transfer.

#### Campuri formular
- cont sursa (selectat din lista conturilor utilizatorului)
- cont destinatie (selectat din lista conturilor utilizatorului)
- suma
- categorie
- descriere

#### Comportament aditional
3. Dupa selectarea contului sursa:
   - lista conturilor destinatie este filtrata, astfel:
     - sunt afisate **doar conturile utilizatorului cu aceeasi valuta**
     - contul sursa nu poate fi selectat ca destinatie

### 4. Validari

- contul sursa trebuie sa fie `ACTIVE`
- contul destinatie trebuie sa fie `ACTIVE`
- utilizatorul nu trebuie sa aiba rol `VIEWER` pe contul sursa
- suma trebuie sa fie pozitiva
- categoria este obligatorie
- contul sursa si contul destinatie trebuie sa fie diferite
- conturile trebuie sa aiba aceeasi valuta

### 5. Flux 2: autorizare transfer

1. Utilizatorul confirma transferul.
2. Aplicatia afiseaza un popup/card de autorizare.
3. Utilizatorul introduce parola de autentificare.

#### Validari
- parola trebuie sa fie corecta
- se verifica limitele tranzactionale:
  - daca exista `USER_LIMITS`, se folosesc acelea
  - altfel, se folosesc `BANK_LIMITS`
- se verifica disponibilitatea fondurilor

### 6. Persistenta si procesare

4. Dupa validari:
   - se creeaza un rand in `TRANSACTIONS`:
     - `initiated_by_user_id`
     - `source_account_id`
     - `destination_account_id`
     - `category_id`
     - `amount`
     - `currency` (valuta contului sursa)
     - `description`
     - `is_urgent = false`
     - `is_scheduled = false`
     - `status = EXECUTED`

### 7. Executie transfer

### Validari finale
- conturile sunt `ACTIVE`
- exista fonduri suficiente in contul sursa

#### Operatii
- suma este scazuta din contul sursa
- suma este adaugata in contul destinatie

### 8. Reguli de business

- transferurile intre conturile proprii sunt executate imediat
- nu exista moduri de procesare standard/urgent/programat ca si la tranzactiile uzuale
- utilizatorii cu rol `VIEWER` nu pot initia transferuri
- conturile implicate trebuie sa fie in aceeasi valuta
- o tranzactie este stocata o singura data
- disponibilitatea fondurilor este verificata la autorizare

### 9. Entitati implicate

- `TRANSACTIONS`
- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `USER_LIMITS`
- `BANK_LIMITS`
- `CATEGORIES`
- `USERS`

### 10. Statusuri relevante

#### TRANSACTIONS
- `EXECUTED`
- `FAILED`

### 11. Rezultat final

- utilizatorul poate transfera fonduri intre propriile conturi
- transferul este executat imediat dupa autorizare
- soldurile conturilor sunt actualizate in timp real