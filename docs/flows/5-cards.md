## Flux management carduri

Documentul descrie operatiunile de vizualizare si creare a cardurilor asociate conturilor bancare.

### 1. Actori implicati
- utilizator autentificat (`USER`)

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`
- el are acces la contul selectat, iar contul este in status `ACTIVE`

### 3. Flux 1: vizualizare card asociat contului

1. Utilizatorul acceseaza pagina de detalii a unui cont.
2. Aplicatia verifica daca exista un card **activ** asociat contului respectiv.

#### Caz card existent
3. Daca exista card **activ**:
   - se afiseaza o imagine a cardului (fara date, doar ca design)
   - se afiseaza un mesaj informativ pentru utilizator, care sa contina si detaliile cardului:

      - numarul cardului (partial mascat)
      - numele titularului
      - data expirarii
      - statusul cardului

#### Caz fara card
4. Daca nu exista card activ:
   - aplicatia afiseaza un buton "Comanda card"

### 4. Flux 2: creare card

1. Utilizatorul apasa pe butonul "Comanda card".
2. Aplicatia valideaza eligibilitatea contului pentru emiterea unui card.

#### Validari
- contul trebuie sa fie in status `ACTIVE`
- utilizatorul nu trebuie sa aiba rol `VIEWER` pe cont (in caz contrar, butonul nu va fi afisat in interfata)
- nu trebuie sa existe deja un card activ asociat contului

#### Persistenta
3. Se creeaza un rand in `CARDS`:
   - asociat contului selectat
   - numar de card generat automat
   - perioada de valabilitate implicita de 10 ani, pentru simplitate
   - tip de card implicit
   - status initial `ACTIVE`
   - numele titularului completat pe baza utilizatorului asociat

4. Dupa creare, cardul devine vizibil in pagina de detalii a contului.

### 5. Reguli de business

- un cont poate avea maximum un card activ
- cardul poate fi emis doar pentru un cont in status `ACTIVE`
- utilizatorul cu rol `VIEWER` poate doar vizualiza datele contului, fara a putea comanda card (butonul nu va fi afisat in interfata)
- daca exista deja un card activ, nu se mai afiseaza optiunea de creare a unui nou card, ci direct informatiile cardului

### 6. Entitati implicate

- `ACCOUNTS`
- `ACCOUNT_ACCESS`
- `CARDS`
- `USERS`

### 7. Statusuri relevante

#### ACCOUNTS
- `ACTIVE`
- `CLOSED`

#### CARDS
- `ACTIVE`
- `BLOCKED`
- `EXPIRED`

### 8. Rezultat final

- utilizatorul poate vizualiza cardul asociat unui cont
- utilizatorul eligibil poate comanda un card nou pentru contul selectat
- cardul creat devine vizibil in interfata imediat dupa salvare