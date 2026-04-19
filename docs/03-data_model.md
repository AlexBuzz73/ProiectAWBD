## 1. Introducere

Modelul de date al aplicatiei a fost construit astfel incat sa sustina functionalitatile principale ale unui sistem de tip Internet Banking destinat persoanelor fizice. Acesta acopera zona de identitate si acces, managementul conturilor si cardurilor, procesarea tranzactiilor, gestionarea limitelor tranzactionale, schimbul valutar si platile programate. Modelul include atat relatii de tip 1:1 si 1:M, cat si relatii de tip M:M. Relatiile M:M sunt utilizate atat pentru functionalitatea de multiaccount, cat si pentru etichetarea tranzactiilor prin tag-uri auxiliare. Diagrama ERD aferenta modelului de date este disponibila in folderul `docs/diagrams/`.

## 2. Tabele principale

### 2.1 INDIVIDUALS

Tabela `INDIVIDUALS` stocheaza datele reale ale persoanei fizice care va utiliza aplicatia. Aceasta separare permite distinctia clara dintre identitatea clientului si contul sau de acces in sistem.

Coloane principale:
- `individual_id` - PK
- `first_name`
- `last_name`
- `cnp`
- `phone_number`
- `date_of_birth`
- `status`
- `created_at`
- `updated_at`

Observatii:
- `cnp` este unic
- `date_of_birth` este folosita pentru validarea varstei minime necesare pentru accesul in sistem. Varsta minima pentru acces este de 18 ani.

### 2.2 USERS

Tabela `USERS` stocheaza datele de autentificare si acces ale utilizatorului.

Coloane principale:
- `user_id` - PK
- `individual_id` - FK catre `INDIVIDUALS`
- `username`
- `email`
- `password_hash`
- `role`
- `failed_login_attempts`
- `status`
- `created_at`
- `updated_at`

Observatii:
- fiecare utilizator este asociat unui singur individ
- `role` defineste rolul utilizatorului la nivelul aplicatiei, de exemplu `USER` sau `ADMIN`
- `failed_login_attempts` este utilizat pentru controlul incercarilor de autentificare esuate
- `email` trebuie sa fie unic

### 2.3 ACCOUNTS

Tabela `ACCOUNTS` stocheaza conturile bancare din sistem.

Coloane principale:
- `account_id` - PK
- `iban`
- `currency`
- `balance`
- `alias`
- `status`
- `created_at`
- `updated_at`

Observatii:
- un cont poate exista in una dintre valutele suportate de sistem
- `iban` trebuie sa fie unic
- relatia dintre utilizatori si conturi este gestionata prin tabela `ACCOUNT_ACCESS`

### 2.4 ACCOUNT_ACCESS

Tabela `ACCOUNT_ACCESS` implementeaza relatia de tip M:M dintre utilizatori si conturi, care sustine functionalitatea de multiaccount.

Coloane principale:
- `account_access_id` - PK
- `account_id` - FK catre `ACCOUNTS`
- `user_id` - FK catre `USERS`
- `access_role`
- `status`
- `created_at`
- `updated_at`

Observatii:
- `access_role` poate avea valori precum `OWNER`, `CO_OWNER` sau `VIEWER`
- utilizatorii cu rol `VIEWER` pot vizualiza contul, dar nu pot autoriza tranzactii
- se va aplica unicitatea pentru combinatia utilizator-cont

### 2.5 CARDS

Tabela `CARDS` stocheaza cardurile asociate conturilor bancare.

Coloane principale:
- `card_id` - PK
- `account_id` - FK catre `ACCOUNTS`
- `card_number`
- `type`
- `expiration_date`
- `holder_name`
- `status`
- `created_at`
- `updated_at`

Observatii:
- un cont poate avea unul sau mai multe carduri asignate
- cardurile sunt emise pentru un cont existent

### 2.6 CATEGORIES

Tabela `CATEGORIES` este utilizata pentru clasificarea tranzactiilor.

Coloane principale:
- `category_id` - PK
- `name`
- `is_system`
- `created_by_user_id` - FK catre `USERS`, optionala
- `status`
- `created_at`
- `updated_at`

Observatii:
- o categorie poate fi definita de sistem sau de utilizator
- daca `is_system` indica 'Y', atunci `created_by_user_id` ramane necompletat
- daca categoria este creata de utilizator (deci is_system = 'N'), `created_by_user_id` este populat cu id-ul acestuia

### 2.7 TRANSACTIONS

Tabela `TRANSACTIONS` reprezinta entitatea centrala a modelului de date, deoarece stocheaza toate operatiunile financiare efectuate in aplicatie.

Coloane principale:
- `transaction_id` - PK
- `initiated_by_user_id` - FK catre `USERS`
- `source_account_id` - FK catre `ACCOUNTS`
- `destination_account_id` - FK catre `ACCOUNTS`, optionala
- `destination_iban`
- `category_id` - FK catre `CATEGORIES`, optionala
- `transaction_type`
- `amount`
- `currency`
- `exchange_rate_id` - FK catre `EXCHANGE_RATES`, optionala
- `description`
- `is_urgent`
- `is_scheduled`
- `status`
- `created_at`
- `updated_at`

Observatii:
- tranzactia este initiata de un utilizator si porneste dintr-un cont sursa
- pentru transferurile interne, `destination_account_id` va fi completat
- pentru platile externe, `destination_iban` va fi folosit in locul unui cont destinatie din sistem
- `is_urgent` indica daca tranzactia trebuie executata imediat dupa autorizare
- `is_scheduled` indica daca tranzactia este asociata unei plati programate
- o tranzactie poate avea asociate unul sau mai multe tag-uri auxiliare, utilizate pentru organizare si filtrare

### 2.8 SCHEDULED_PAYMENTS

Tabela `SCHEDULED_PAYMENTS` stocheaza informatiile specifice platilor programate.

Coloane principale:
- `scheduled_payment_id` - PK
- `transaction_id` - FK catre `TRANSACTIONS`
- `scheduled_date`
- `status`
- `created_at`
- `updated_at`

Observatii:
- o plata programata este asociata unei tranzactii existente
- aceasta tabela contine doar datele suplimentare necesare programarii
- executia este realizata de un job automat, in functie de `scheduled_date`

### 2.9 BANK_LIMITS

Tabela `BANK_LIMITS` stocheaza limitele globale impuse de sistem.

Coloane principale:
- `bank_limit_id` - PK
- `max_amount_per_transaction_ron`
- `max_daily_amount_ron`
- `max_daily_transactions_count`
- `status`
- `created_at`
- `updated_at`

Observatii:
- limitele din aceasta tabela sunt configurate **doar de utilizatori** cu rol `ADMIN`
- sistemul va functiona cu un singur set activ de limite globale
- toate valorile sunt exprimate in RON

### 2.10 USER_LIMITS

Tabela `USER_LIMITS` stocheaza limitele configurabile de fiecare utilizator.

Coloane principale:
- `user_limit_id` - PK
- `user_id` - FK catre `USERS`
- `max_amount_per_transaction_ron`
- `max_daily_amount_ron`
- `max_daily_transactions_count`
- `status`
- `created_at`
- `updated_at`

Observatii:
- un utilizator poate avea sau nu propriul set de limite
- daca pentru un utilizator nu exista un rand in aceasta tabela, sistemul va utiliza implicit limitele globale din `BANK_LIMITS`
- valorile din `USER_LIMITS` nu pot depasi valorile din `BANK_LIMITS`

### 2.11 EXCHANGE_RATES

Tabela `EXCHANGE_RATES` stocheaza cursurile valutare utilizate de sistem.

Coloane principale:
- `exchange_rate_id` - PK
- `currency_from`
- `currency_to`
- `rate`
- `rate_date`
- `source`
- `created_at`

Observatii:
- cursurile sunt preluate periodic dintr-un API extern pus la dispozitie de BNR
- sistemul va utiliza doar ratele de schimb `USD -> RON` si `EUR -> RON`
- tabela este folosita pentru furnizarea cursurilor de schimb valutar, utilizate atat in operatiunile de schimb valutar, cat si pentru validarea limitelor tranzactionale in moneda de referinta (RON)
- pentru tranzactiile de tip schimb valutar, in tabela `TRANSACTIONS` se va salva referinta catre rata utilizata, prin coloana `exchange_rate_id`
- pentru conversiile in sens invers (`RON -> USD`, `RON -> EUR`), sistemul va utiliza aceeasi rata de baza, aplicand formula inversa in logica aplicatiei
- pentru aceeasi zi si aceeasi pereche valutara se va impune unicitatea

### 2.12 TAGS

Tabela `TAGS` stocheaza etichete auxiliare utilizate pentru organizarea si filtrarea tranzactiilor.

Coloane principale:
- `tag_id` - PK
- `name`
- `status`
- `created_at`
- `updated_at`

Observatii:
- tag-urile sunt predefinite in sistem
- acestea nu pot fi create sau modificate de utilizatori
- un tag poate fi asociat mai multor tranzactii
- tag-urile sunt utilizate pentru clasificare secundara si filtrare in istoricul tranzactiilor

### 2.13 TRANSACTION_TAGS

Tabela `TRANSACTION_TAGS` implementeaza relatia de tip M:M dintre `TRANSACTIONS` si `TAGS`.

Coloane principale:
- `transaction_id` - FK catre `TRANSACTIONS`
- `tag_id` - FK catre `TAGS`

Observatii:
- o tranzactie poate avea zero, unul sau mai multe tag-uri
- aceeasi eticheta poate fi asociata mai multor tranzactii
- tabela este utilizata pentru organizarea suplimentara a tranzactiilor si pentru filtrare in interfata utilizatorului


## 3. Reguli business

Modelul de date reflecta urmatoarele reguli importante de business:
- un utilizator nu poate exista fara un individ asociat
- un cont poate fi accesat de mai multi utilizatori prin mecanismul de multiaccount
- un utilizator cu rol `VIEWER` in `ACCOUNT_ACCESS` nu poate autoriza tranzactii
- o tranzactie poate fi standard, urgenta sau programata pentru executie ulterioara
- o plata programata exista atat in `TRANSACTIONS`, cat si in `SCHEDULED_PAYMENTS`
- limitele utilizatorului nu pot depasi limitele globale impuse de sistem
- daca un utilizator nu are limite proprii, se aplica automat limitele globale
- toate limitele valorice sunt exprimate in RON
- pentru tranzactiile in alta valuta, valorile sunt convertite folosind cursul disponibil in sistem
- categoriile pot fi create de sistem sau create de utilizator
- o tranzactie poate avea asociate zero, unul sau mai multe tag-uri auxiliare
- tag-urile sunt predefinite si utilizate pentru clasificare secundara si filtrare
- tag-urile **nu inlocuiesc categoria principala a tranzactiei**