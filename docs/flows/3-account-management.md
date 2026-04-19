## Flux management conturi bancare

Documentul descrie operatiunile pe care un utilizator le poate realiza asupra conturilor bancare, inclusiv creare, vizualizare, acces multiaccount si inchidere cont.

### 1. Actori implicati
- utilizator autentificat (`USER`)
- utilizator cu rol `ADMIN` (pentru operatiuni specifice multiaccount)

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`

### 3. Flux 1: creare cont single

1. Utilizatorul selecteaza din dashboard optiunea "cont nou"
2. Aplicatia afiseaza formularul de creare cont, cu urmatoarele campuri de completat:
   - alias cont
   - valuta (RON/USD/EUR)

3. Utilizatorul completeaza datele si confirma.

#### Validari
- alias-ul este obligatoriu
- valuta trebuie selectata din lista suportata

#### Persistenta
4. Se creeaza un rand in `ACCOUNTS`:
   - IBAN generat automat
   - sold initial 0
   - status = `ACTIVE`

5. Se creeaza un rand in `ACCOUNT_ACCESS`:
   - userul curent
   - rol = `OWNER`

### 4. Flux 2: creare cont multiaccount

#### Caz USER
1. Utilizatorul selecteaza din dashboard optiunea "cont nou"
2. In cadrul ecranului, aplicatia afiseaza mesaj:
   - "Pentru crearea unui cont partajat va rugam sa contactati banca."
3. Fluxul se opreste.

#### Caz ADMIN
4. Utilizatorul cu rol `ADMIN` acceseaza functionalitatea dedicata, din dashboard-ul sau.
5. Aplicatia afiseaza formularul:
   - alias cont
   - valuta (RON/USD/EUR)
   - lista utilizatori (selectare dupa email)

6. ADMIN selecteaza maxim 2 utilizatori si rolurile acestora:
   - `OWNER`
   - `CO_OWNER`
   - `VIEWER`

#### Validari
- maxim 2 utilizatori per cont
- fiecare utilizator trebuie sa existe
- trebuie sa existe cel putin un `OWNER`

#### Persistenta
7. Se creeaza contul in `ACCOUNTS`
8. Se creeaza intrari in `ACCOUNT_ACCESS` pentru fiecare utilizator

### 5. Flux 3: vizualizare conturi

1. Utilizatorul vizualizeaza lista conturilor din dashboard
2. Lista include toate conturile unde exista mapping activ in `ACCOUNT_ACCESS`

### 6. Flux 4: vizualizare detalii cont

1. Utilizatorul selecteaza un cont
2. Aplicatia afiseaza:
   - alias cont
   - IBAN
   - sold
   - lista tranzactii

#### Functionalitati
- tranzactiile sunt paginate
- se poate face sortare:
  - dupa data
  - dupa suma
  - ascendent / descendent

#### Restrictii roluri
- `VIEWER`:
  - poate doar vizualiza
  - nu poate initia tranzactii
  - nu poate selecta contul ca sursa in fluxurile de tranzactii

- `OWNER` / `CO_OWNER`:
  - pot initia tranzactii

### 7. Flux 5: revocare acces cont

1. Operatiunea este disponibila doar pentru `ADMIN` pe conturile partajate
2. ADMIN selecteaza un cont multiaccount
3. Selecteaza utilizatorul ce trebuie eliminat

#### Validari
- nu se poate elimina ultimul `OWNER`

#### Persistenta
4. Se dezactiveaza randul din `ACCOUNT_ACCESS`, iar utilizatorul nu mai vede contul in dashboard

### 8. Flux 6: inchidere cont

1. Utilizatorul acceseaza detaliile unui cont
2. Selecteaza optiunea "inchide cont"

#### Validari
- contul trebuie sa fie in status `ACTIVE`
- soldul contului trebuie sa fie 0
- utilizatorul trebuie sa aiba rol `OWNER`

#### Persistenta
3. Statusul contului devine `CLOSED`

#### Observatii
- inregistrarile din `ACCOUNT_ACCESS` nu sunt sterse, doar invalidate (prin status)
- contul nu mai poate fi utilizat in operatiuni noi
- contul ramane vizibil pentru istoric (ex: istoric tranzactii)

### 9. Reguli de business

- un cont poate avea maxim 2 utilizatori activi
- doar `ADMIN` poate crea conturi multiaccount
- `VIEWER` nu poate initia sau autoriza tranzactii (nu poate selecta contul)
- rolurile pe cont nu pot fi modificate ulterior
- un cont poate fi inchis doar de un `OWNER` si doar daca soldul este 0
- un cont `CLOSED` nu va mai fi afisat in lista de conturi acitve (dashboard) si nici nu poate fi utilizat in operatiuni noi

### 10. Entitati implicate

- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `USERS`
- `TRANSACTIONS`

### 11. Statusuri relevante

#### ACCOUNTS
- `ACTIVE`
- `CLOSED`

#### ACCOUNT_ACCESS
- `ACTIVE`
- `INACTIVE`

### 12. Rezultat final

- utilizatorul poate crea si gestiona conturi
- utilizatorul poate vizualiza tranzactiile asociate conturilor
- accesul la conturi este controlat pe baza rolurilor
- conturile pot fi inchise conform regulilor definite