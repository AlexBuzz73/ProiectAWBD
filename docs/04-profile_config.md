# Configurare profile Spring & baza de date

Aplicatia utilizeaza 3 profile Spring pentru a simula mediile de rulare ale unei aplicatii reale. Fiecare profil este configurat sa foloseasca o baza de date diferita, astfel incat comportamentul aplicatiei sa fie cat mai apropiat de scenarii reale de utilizare. Cele 3 medii configurate sunt:
- dev
- test
- prod

Pentru persistenta datelor aplicatia foloseste doua tipuri de baze de date: MySQL, pentru mediile dev si prod, H2 in-memory, pentru mediul test. Configurarea este urmatoarea:
- dev: baza de date `awbd_dev` (MySQL)
- test: baza de date `testdb` (H2 in-memory)
- prod: baza de date `awbd_prod` (MySQL)

## Docker setup (MySQL)

Serverul MySQL ruleaza intr-un container Docker definit in fisierul `docker-compose.yml`. Aditional, pentru persistenta datelor, containerul foloseste un volum mapat pe directorul intern utilizat de MySQL pentru stocarea datelor, `/var/lib/mysql`, ceea ce permite persistenta chiar si dupa oprirea containerului.

Baza de date `awbd_dev` este creata automat la pornirea containerului, fiind definita in configurarea Docker. Baza de date `awbd_prod` trebuie creata manual o singura data (va ramane persistata datorita volumului), iar pentru a face acest lucru, se vor urma pasii:

1. Conectare la containerul MySQL:

```bash
docker exec -it awbd-mysql mysql -u root -p
```

2. Crearea bazei de date:

```sql
CREATE DATABASE awbd_prod;
```

3. Acordarea drepturilor pentru utilizatorul aplicatiei:

```sql
GRANT ALL PRIVILEGES ON awbd_prod.* TO 'awbd'@'%';
FLUSH PRIVILEGES;
```

## Initializarea datelor pentru profilul `test`

Pentru a permite testarea rapida a functionalitatilor, a fost definit fisierul `data-test.sql` care contine un set simplu de date. Fisierul este incarcat automat la fiecare pornire a aplicatiei pe profilul `test`, prin configurarea explicita a locatiei sale in fisierul aferent de proprietati.