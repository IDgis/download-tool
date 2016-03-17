
/**
 * Download tool voor het Geoportaal.<br>
 * 
 * <pre>

DownloadTool project structuur
==============================

---------------------------------------
Gradle multi project met docker images
---------------------------------------
domain                  // dependencies naar alle andere subprojecten
dao                     // dependencies naar subproject userinterface
downloader              // docker: processor, downloaders, packager
						// dependencies: idgis.commons.convert, idgis.commons.cache,  
userinterface
    downloadrequester   // docker: play applicatie
                        // configuratie: view (tekst, logo, i18n)
    feedback            // docker: 2 containers met aparte configuratie voor OK en NOK
                        // configuratie: email templates
geoportaalinterface     // MetadataToDownloadBeanConverter
                        // dependencies naar userinterface.downloadrequester
queue                   // docker: queue client met beanstalk
                        // dependencies naar subprojects userinterface, downloader
-----------------
Package structuur
-----------------
nl.idgis.downloadtool
    .domain             //  domein beans :
                            Download bean
                            DownloadRequest bean
                            Feedback bean
                            WfsFeatureType, AdditionalData
                            
    .dao                    // dao class
                            DownloadDao
                            DownloadRequestInfo
                            DownloadResponseInfo
    .userinterface
        .downloadrequester  // Play controller
        .feedback           // Feedbackprovider
    .downloader             // Processor, WFS downloader, converter, packager
    .queue                  // DownloadQueueClient used by DownloadRequester, FeedbackProvider, Processor
    .geoportaalinterface    // MetadataToDownloadBeanConverter

-------------
Docker images
-------------
downloader          // docker: processor, downloaders, packager
downloadrequester   // docker: play application
feedbackprovider    // docker: 2 containers met aparte configuratie voor OK en NOK feedback queues
store               // docker: disk volume  
database            // 
beanstalk 			// beanstalkd

 * </pre>
 * 
 *  <img src="DownloadToolSmall.png">
 * 
 * @author Rob
 *
 */
package nl.idgis.downloadtool;