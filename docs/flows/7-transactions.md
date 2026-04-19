## Flux plati normale (interne/externe)

Documentul descrie operatiunile de initiere si procesare a tranzactiilor uzuale, care pot fi:
- interne, catre conturi existente in sistem/banca
- externe, catre conturi din afara sistemului/bancii

Platile pot fi procesate in mod:
- standard
- urgent
- programat

### 1. Actori implicati
- utilizator autentificat (`USER`)

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`
- utilizatorul are acces la contul sursa (rol `OWNER` sau `CO_OWNER`)
- contul sursa este in status `ACTIVE`

### 3. Flux 1: initiere plata

1. Utilizatorul acceseaza optiunea "Plata noua" din dashboard.
2. Aplicatia afiseaza formularul de initiere plata.

#### Campuri formular
- cont sursa (selectat dintr-o lista ce contine conturile utilizatorului)
- IBAN destinatie
- suma
- valuta
- categorie (selectata din lista categoriilor de sistem si create de utilizator)
- tip procesare:
  - `STANDARD`
  - `URGENT`
  - `PROGRAMAT`
- descriere
- tag-uri (selectate optional dintr-o lista predefinita in tabela aferenta)

#### Comportament aditional
3. Daca utilizatorul selecteaza tipul `PROGRAMAT`:
   - apare un camp suplimentar:
     - data executiei (date picker), unde data minima selectabila este ziua urmatoare

### 4. Validari initiale

- contul sursa trebuie sa fie `ACTIVE`
- utilizatorul nu trebuie sa aiba rol `VIEWER`
- suma trebuie sa fie pozitiva
- categoria este obligatorie
- IBAN-ul destinatie este obligatoriu
- valuta tranzactiei va corespunde cu valuta contului sursa
- daca tipul este `PROGRAMAT`:
  - data trebuie sa fie valida si in viitor
- daca sunt selectate tag-uri, acestea trebuie sa existe in lista predefinita din sistem

### 5. Determinare tip plata

1. Dupa confirmarea si autorizarea platii, aplicatia verifica daca IBAN-ul destinatie exista in sistem:
   - daca exista, atunci plata este **interna**
   - daca nu exista, atunci plata este **externa**

### 6. Flux 2: autorizare plata

1. Dupa completarea formularului, utilizatorul confirma plata.
2. Aplicatia afiseaza un popup/card de autorizare.
3. Utilizatorul introduce parola de autentificare.

#### Validari
- parola trebuie sa fie corecta
- se verifica limitele tranzactionale:
  - daca exista `USER_LIMITS`, se folosesc acelea
  - altfel, se folosesc `BANK_LIMITS`
- se verifica si disponibilitatea fondurilor

### 7. Persistenta initiala

4. Dupa validari:
   - se creeaza un rand in `TRANSACTIONS`:
     - `initiated_by_user_id`
     - `source_account_id`
     - `destination_iban`
     - `destination_account_id` (daca este plata interna)
     - `category_id`
     - `amount`
     - `currency`
     - `description`
     - `is_urgent`
     - `is_scheduled`
     - `status = DRAFT`

   - daca utilizatorul a selectat tag-uri:
     - se creeaza intrari in tabela `TRANSACTION_TAGS` pentru fiecare tag selectat, dupa crearea tranzactiei in sistem

### 8. Procesare tranzactie

#### Caz 1: plata urgenta
- status: `DRAFT` -> `AUTHORIZED` -> `EXECUTED`
- tranzactia este executata imediat

#### Caz 2: plata standard
- status: `DRAFT` -> `AUTHORIZED` -> `PENDING_EXECUTION`
- tranzactia va fi executata ulterior de un job automat (pentru simplitate, se va executa dupa 2 minute)

#### Caz 3: plata programata
- status: `DRAFT` -> `AUTHORIZED` -> `PENDING_EXECUTION`
- aditional, se creeaza un rand in `SCHEDULED_PAYMENTS`:
  - `transaction_id`
  - `scheduled_date`
- executia se face la data specificata printr-un job ce va rula zilnic

### 9. Executie tranzactie

La executie (indiferent de tip):

#### Validari finale
- contul sursa este inca `ACTIVE`
- exista fonduri suficiente

#### Operatii
- suma este scazuta din contul sursa
- daca plata este interna:
  - suma este adaugata in contul destinatie

#### Rezultat
- status devine `EXECUTED`
- in caz de eroare, status devine `FAILED`

### 10. Reguli de business

- utilizatorii cu rol `VIEWER` nu pot initia plati
- categoria este obligatorie
- limitele sunt verificate la autorizare
- daca nu exista limite proprii, se aplica limitele globale
- platile interne sunt determinate automat pe baza IBAN-ului
- o tranzactie este stocata o singura data, chiar daca afecteaza doua conturi
- disponibilitatea fondurilor este verificata la autorizare si, pentru tranzactiile care nu sunt executate imediat, si la momentul executiei finale
- platile standard si programate sunt executate prin joburi automate
- platile urgente sunt executate imediat
- asocierea tag-urilor este optionala
- tag-urile sunt utilizate doar pentru organizare si filtrare si nu influenteaza procesarea tranzactiei

### 11. Entitati implicate

- `TRANSACTIONS`
- `SCHEDULED_PAYMENTS`
- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `USER_LIMITS`
- `BANK_LIMITS`
- `CATEGORIES`
- `USERS`
- `TAGS`
- `TRANSACTION_TAGS`

### 12. Statusuri relevante

#### TRANSACTIONS
- `DRAFT`
- `AUTHORIZED`
- `PENDING_EXECUTION`
- `EXECUTED`
- `FAILED`

#### SCHEDULED_PAYMENTS
- `ACTIVE`
- `EXECUTED`
- `FAILED`

### 13. Rezultat final

- utilizatorul poate initia plati interne sau externe
- platile pot fi executate imediat sau ulterior
- tranzactiile sunt validate pe baza limitelor si soldului
- sistemul gestioneaza executia automata a platilor standard si programate