# Download tool voor het Geoportaal.

## Beschrijving

De downloadtool is onderdeel van het Geoportaal Overijssel.
Het geeft de mogelijkheid om datasets in verschillende formaten om te laten zetten en samen met metadata in een zip te laten verpakken.
De zip file kan worden gedownload, de download link wordt in een vervolgscherm getoond.

Stappen:

1. Klik bij de gegevens van een dataset op de download link.
2. Een formulier wordt getoond, kies het gewenste formaat.
3. De dataset wordt omgezet in het gewenste formaat en in een zip file verpakt.
4. Indien dit gereed is wordt in een vervolgscherm de link naar de de zip file getoond.

## Project structuur

### Gradle multi project met docker images

| project | description |
| --- | --- |
| dao            | CRUD methoden database | 
| db             | docker: postgresql 9.x database |
| domain         | domein objecten: message beans, database tabellen  |
| downloader     | docker: downloader met processor, downloaderWfs, downloadFile, packager |
|                | dependencies: idgis.commons.cache voor zip packager |  
| gc             | docker: Garbage collection: opruimen van gedownloade zip bestanden |
| queue          | docker: queue client met beanstalk daemon |
| web            | docker: play applicatie |
|                | DownloadForm, MetadataProvider |


### Docker containers en images

| image | container | beschrijving |
| --- | --- | --- | 
| beanstalk | schickling/beanstalkd | queue daemon |
| downloader | downloader | downloaderWfs, downloadFile, packager | 
| db | db | database met tabellen request\_info en result\_info |
| gc | gc | disk volume voor tijdelijke opslag zip files | 
| web | play application | web formulier, metadata provider, download proxy | 
| cache | downloader | disk volume voor tijdelijke opslag zip files | 
