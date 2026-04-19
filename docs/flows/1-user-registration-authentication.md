## Flux inregistrare utilizator si autentificare

Fluxul permite crearea unui cont de acces in aplicatie pentru o persoana fizica eligibila si autentificarea ulterioara in sistem.

### 1. Actori implicati
- utilizator neautentificat
- utilizator cu rol `ADMIN` (doar pentru deblocarea conturilor, daca este cazul)

### 2. Preconditii
- utilizatorul nu este deja inregistrat in sistem
- CNP-ul introdus nu exista deja in tabela `INDIVIDUALS`
- email-ul introdus nu exista deja in tabela `USERS`
- persoana trebuie sa aiba _**cel putin 18 ani**_

### 3. Flux 1: inregistrarea unui individ in aplicatie

1. Utilizatorul acceseaza aplicatia si vizualizeaza ecranul de logare.
2. Selecteaza optiunea de creare cont.
3. Aplicatia afiseaza formularul pentru datele individului, ce contine campuri pentru:
   - nume
   - prenume
   - CNP
   - numar telefon
   - data nastere
4. Acesta completeaza datele si confirma.

#### Validari
- toate campurile sunt obligatorii
- CNP-ul trebuie sa fie unic
- se verifica varsta minima de 18 ani

#### Rezultat
- daca validarea esueaza
   - se afiseaza mesaj de eroare si fluxul se opreste
- daca validarea reuseste 
   - utilizatorul trece la pasul urmator

- Observatie: datele din acest pas NU SUNT INCA persistate in baza de date. Ele sunt pastrate temporar pana la finalizarea completa a fluxului, folosind un DTO care contine atat datele individului, cat si datele utilizatorului.

#### Creare utilizator
5. In continuare, aplicatia afiseaza formularul pentru setarea credentialelor:
   - username
   - email
   - parola
6. Utilizatorul completeaza datele si confirma.

#### Validari
- email unic
- username unic
- parola respecta reguli minime de securitate si nu este stocata in clar

#### Persistenta datelor
7. Dupa validarea tuturor datelor:
   - se creeaza un rand in `INDIVIDUALS`
   - se creeaza un rand in `USERS`, asociat individului tocmai persistat
8. Parola este stocata sub forma hash (BCrypt)
9. Utilizatorul este creat _*cu rol implicit `USER`*_
10. Statusul initial este `ACTIVE`

#### Final flux inregistrare
11. Utilizatorul este redirectionat la pagina de logare

---

### 4. Flux 2: autentificare

1. Utilizatorul introduce email-ul si parola
2. Aplicatia cauta utilizatorul dupa email

#### Validari
3. Se verifica existenta utilizatorului
4. Se verifica statusul:
   - daca status = `BLOCKED` sau `CLOSED`, autentificarea este refuzata
5. Se verifica parola (comparatie hash)

#### Rezultat

##### Caz succes
6. Contorul `failed_login_attempts` se reseteaza la 0
7. Utilizatorul este redirectionat catre dashboard

##### Caz esec
8. Contorul `failed_login_attempts` este incrementat

### 5. Reguli de blocare

- dupa 3 incercari esuate:
  - statusul utilizatorului devine `BLOCKED`
  - autentificarea nu mai este permisa
  - se afiseaza mesaj:
    - "Contul a fost blocat. Va rugam sa contactati banca."

- contorul de incercari este cumulativ si se reseteaza doar la autentificare reusita sau _*la deblocarea efectuata de un ADMIN*_

### 6. Deblocare utilizator

- doar un utilizator cu rol `ADMIN` poate debloca un cont
- deblocarea presupune:
  - setarea statusului la `ACTIVE`
  - resetarea contorului `failed_login_attempts`

### 7. Reguli de business

- un utilizator nu poate exista fara un individ asociat
- varsta minima pentru inregistrare este 18 ani
- CNP-ul trebuie sa fie unic
- email-ul trebuie sa fie unic
- parola nu se stocheaza in clar
- dupa 3 incercari esuate, utilizatorul este blocat
- utilizatorii blocati nu se pot autentifica
- doar ADMIN poate debloca utilizatorii

### 8. Entitati implicate

- `INDIVIDUALS`
- `USERS`

### 9. Statusuri relevante

#### USERS
- `ACTIVE`
- `BLOCKED`
- `CLOSED`

#### INDIVIDUALS
- `ACTIVE`
- `BLOCKED`
- `CLOSED`

### 10. Rezultat final

- utilizatorul isi poate crea cont de acces in sistem
- utilizatorul se poate autentifica si accesa dashboard-ul
- utilizatorii blocati nu pot accesa sistemul pana la deblocare