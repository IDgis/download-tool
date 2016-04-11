

#Download tool voor het Geoportaal.

##DownloadTool project structuur

###Gradle multi project met docker images

| project | description |
| --- | --- |
| dao            | CRUD methoden database | 
| db             | docker: postgresql 9.x database |
| domain         | domein objecten: message beans, database tabellen  |
| downloader     | docker: downloader met processor, downloaderWfs, downloadFile, packager |
|                | dependencies: idgis.commons.cache voor zip packager |  
| feedback       | 2 docker containers met aparte configuratie voor OK en NOK situaties |
|                | configuratie: email templates |
|                | dependencies: idgis.commons.utils voor Mail  |
| gc             | docker: Garbage collection: opruimen van gedownloade zip bestanden |
| queue          | docker: queue client met beanstalk daemon |
| web            | docker: play applicatie |
|                | DownloadForm, MetadataProvider |


###Docker containers en images

```
| image | container | beschrijving |
| --- | --- | --- | 
| beanstalk | schickling/beanstalkd | queue daemon |
| downloader | downloader | downloaderWfs, downloadFile, packager | 
| feedback | feedback\_ok en feedback\_error | feedback queues voor OK en NOK situaties | 
| db | db | database met tabellen request\_info en result\_info |
| gc | gc | disk volume voor tijdelijke opslag zip files | 
| web | play application | web formuloier, metadata provider, download proxy | 
| cache | downloader | disk volume voor tijdelijke opslag zip files | 

```
