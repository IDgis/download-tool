/**
 * 
 */
package nl.idgis.downloadtool.downloadrequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueueClient;

/**
 * @author Rob
 *
 */
public class DownloadRequestController {
	private static final Logger log = LoggerFactory.getLogger(DownloadRequestController.class);

	private static final String BEANSTALK_HOST = "BEANSTALK_HOST";
	private static final String BEANSTALK_DOWNLOAD_QUEUE = "BEANSTALK_DOWNLOAD_QUEUE";
	
//	private DownloadQueue queueClient;

	/**
	 * temporary test method to inject DownloadRequests every few seconds.
	 * @param args
	 */
	public static void main(String[] args) {
		String host = System.getenv(BEANSTALK_HOST);
		if(host == null) {
			host = "localhost";
		}
		String downloadQueueTubeName = System.getenv(BEANSTALK_DOWNLOAD_QUEUE);
		if(downloadQueueTubeName == null) {
			downloadQueueTubeName = "downloadRequestTube";
		}
		
		try {
			log.info("start loop ");
			
			// setup queue client
			DownloadQueueClient downloadQueueClient = new DownloadQueueClient(host, downloadQueueTubeName);
			DownloadRequestController fbp = new DownloadRequestController();
			
			for (;;) {
				Thread.sleep(10000);
				
				String uuid = UUID.randomUUID().toString();
				DownloadRequest downloadRequest = new DownloadRequest(uuid);
				log.debug("processDownloadRequest " + downloadRequest);
				
				Download download = new Download();
				download.setName("download");
				WfsFeatureType ft = new WfsFeatureType();
				ft.setCrs("EPSG:28992");
				ft.setName("Featuretype");
				ft.setExtension("kml");
				ft.setServiceUrl("http://httpbin.org/post");
				ft.setServiceVersion("2.0.0");
				ft.setWfsMimetype("KML");
				download.setFt(ft);
				AdditionalData additionalData = new AdditionalData();
				additionalData.setName("someData");
				additionalData.setExtension("txt");
				additionalData.setUrl("http://httpbin.org/get");
				List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
				additionalDataList.add(additionalData);
				download.setAdditionalData(additionalDataList);
				downloadRequest.setDownload(download);
				downloadRequest.setConvertToMimetype("KML");
				
				downloadQueueClient.sendDownloadRequest(downloadRequest);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("end loop ");
		}


	}

}
