# Lennukirakendus
Lennukirakendus CGI suvepraktikale kandideerimiseks.

# Kuidas jooksutada?
1. Clone'i giti repo
2. veendu et docker jookseb
3. liigu terminalis clone'itud projekti kausta
4. jooksuta ``` mvn clean package ```
5. jooksuta ``` docker-compose build ``` 
6. jooksuta ``` docker-compose up ``` 

# Projekti valmimise protsess
Kokku läks projektile aega umbes viis õhtut (kokku umbes 24 tundi). Palju aega läks alguses pusimisele, sest ma arvasin, et suudan Springbooti lihtsalt nullist kasutama hakata, mis tegelikult ei olnud realistlik. Siis vaatasin ühte videot Springbooti kohta ja edasi läks protsess palju valutumalt. Kõige suurem probleem projekti juures esines, kui proovisin kasutada dockeris hostitud postgreSQL database'i, aga äpp ei suutnud kuidagi sinna connectida. Probleemi lahendamiseks käisin läbi enamvähem kõik stackoverflow foorumid, mis probleemi mainisid. Lõpuks leidsin kuidas keegi kirjutas, et temal oli probleemiks windows 10 taustal jooksev postgreSQL service, mis osutuski probleemiks. Äpp proovis järelikult ühenduda minu arvuti postgreSQL serveriga ja mitte Dockeri omaga.

Kogu backendi kood on projektis minu enda kirjutatud, chatGPT'd kasutasin, et luua HTML'i boilerplate, kuhu siis ma ise vajalikud elemendid lisasin ja et luua lehele algne stiil opencssi'ga.
