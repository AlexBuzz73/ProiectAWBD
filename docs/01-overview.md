## 1. Introducere

Aplicatia reprezinta un sistem de tip Internet Banking destinat persoanelor fizice. Aceasta permite utilizatorilor sa isi gestioneze conturile bancare, sa efectueze tranzactii si sa isi configureze setari financiare intr-un mod centralizat. Scopul este de a simula functionalitatile de baza ale unui sistem bancar digital, oferind un flux intuitiv pentru operatiuni financiare uzuale.

## 2. Context

In contextul digitalizarii serviciilor financiare, utilizatorii au nevoie de aplicatii care sa le permita acces rapid si sigur la conturile lor bancare. Aplicatia raspunde acestei nevoi prin furnizarea unui sistem care permite gestionarea completa a conturilor, efectuarea de tranzactii si automatizarea anumitor procese financiare.

## 3. Functionalitati incluse

Aplicatia permite:
- inregistrarea utilizatorilor in sistem
- autentificarea utilizatorilor
- gestionarea conturilor bancare
- emiterea si administrarea cardurilor
- efectuarea tranzactiilor
- configurarea limitelor tranzactionale
- realizarea schimburilor valutare
- programarea platilor pentru executie ulterioara
- acces la conturi partajate intre utilizatori

## 4. Actorii sistemului

### USER

Reprezinta utilizatorul standard al aplicatiei, care poate:
- sa isi creeze si gestioneze conturile
- sa efectueze tranzactii
- sa isi configureze limitele
- sa acceseze conturi proprii sau partajate

### ADMIN

Reprezinta utilizatorul cu drepturi administrative, care poate:
- gestiona limitele globale ale sistemului
- vizualiza si administra datele aplicatiei
- gestiona utilizatorii si conturile acestora

## 5. Module

Aplicatia este organizata in jurul urmatoarelor module:

- managementul identitatii utilizatorilor
- managementul conturilor si cardurilor
- procesarea tranzactiilor
- gestionarea limitelor tranzactionale
- schimb valutar
- plati programate
- controlul accesului la conturi (multiaccount)

## 6. Flux General

1. Un individ este inregistrat in sistem
2. Se creeaza un utilizator asociat acestuia
3. Utilizatorul se autentifica in aplicatie
4. Utilizatorul isi creeaza unul sau mai multe conturi bancare
5. Pot fi emise carduri asociate conturilor
6. Utilizatorul initiaza tranzactii
7. Tranzactiile sunt validate in functie de reguli si limite
8. Tranzactiile sunt executate imediat sau programate pentru executie ulterioara