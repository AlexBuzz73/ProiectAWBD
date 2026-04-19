## Flux management categorii tranzactii

Documentul descrie operatiunile de vizualizare, creare si stergere a categoriilor utilizate pentru clasificarea tranzactiilor.

### 1. Actori implicati
- utilizator autentificat (rol `USER`)

### 2. Preconditii
- utilizatorul este autentificat si are status `ACTIVE`

### 3. Flux 1: vizualizare categorii

1. Utilizatorul acceseaza optiunea de vizualizare categorii din dashboard.
2. Aplicatia redirectioneaza utilizatorul catre ecranul de categorii, unde este afisata lista categoriilor disponibile pentru utilizator.

#### Reguli de afisare
- sunt afisate **toate categoriile de sistem**:
  - `is_system = Y`
- sunt afisate si categoriile **create de utilizatorul curent**:
  - `created_by_user_id = user curent`

### 4. Flux 2: creare categorie

1. Utilizatorul apasa butonul "Creeaza o categorie" din ecranul cu lista categoriilor.
2. Aplicatia afiseaza formularul de creare categorie.
3. Utilizatorul completeaza campul:
   - `name`

4. Acesta confirma crearea categoriei.

#### Validari
- numele categoriei este obligatoriu si **nu poate coincide cu numele unei categorii de sistem, nici cu numele unei categorii deja create de utilizatorul respectiv**

#### Persistenta
5. Se creeaza un rand in `CATEGORIES`:
   - `name` = valoarea completata
   - `is_system` = `N`
   - `created_by_user_id` = utilizatorul curent
   - `status` = `ACTIVE`

### 5. Flux 3: stergere categorie

1. Aplicatia afiseaza optiunea de stergere in dreptul categoriilor create de user.
2. Utilizatorul apasa butonul de stergere a unei categorii.
3. Acesta confirma stergerea.

#### Validari
- utilizatorul poate sterge doar categoriile create de el
- categoriile de sistem nu pot fi sterse

#### Persistenta
4. Categoria este marcata cu status `INACTIVE`

### 6. Reguli de business

- categoriile de sistem sunt vizibile pentru toti utilizatorii
- categoriile create de utilizator sunt vizibile doar utilizatorului care le-a creat si **pot fi folosite doar de acesta** in tranzactii
- un utilizator poate crea/sterge categorii proprii
- categoriile de sistem nu pot fi sterse sau modificate de utilizatorul standard

### 7. Entitati implicate

- `CATEGORIES`
- `USERS`

### 8. Statusuri relevante

#### CATEGORIES
- `ACTIVE`
- `INACTIVE`

### 9. Rezultat final

- utilizatorul poate vizualiza categoriile disponibile pentru clasificarea tranzactiilor
- utilizatorul poate crea categorii proprii
- utilizatorul poate sterge categoriile create de el