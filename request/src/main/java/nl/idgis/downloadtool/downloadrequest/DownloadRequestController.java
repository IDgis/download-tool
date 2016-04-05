/**
 * 
 */
package nl.idgis.downloadtool.downloadrequest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;

/**
 * @author Rob
 *
 */
public class DownloadRequestController {
	private static final Logger log = LoggerFactory.getLogger(DownloadRequestController.class);

	private static final String BEANSTALK_HOST = "BEANSTALK_HOST";
	private static final String BEANSTALK_DOWNLOAD_QUEUE = "BEANSTALK_DOWNLOAD_QUEUE";

	private static final String DB_URL = "DB_URL";
	private static final String DB_USER = "DB_USER";
	private static final String DB_PW = "DB_PW";
	
	private DownloadDao downloadDao;
	
	private DownloadQueue queueClient;

	public DownloadRequestController(DownloadQueue queueClient, DownloadDao downloadDao) {
		super();
		this.queueClient = queueClient;
		this.downloadDao = downloadDao;
	}

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
		String dbUser = System.getenv(DB_USER);
		if (dbUser==null){
			dbUser = "postgres";
		}
		String dbPassword = System.getenv(DB_PW);
		if (dbPassword==null){
			dbPassword = "postgres";
		}
		String dbUrl = System.getenv(DB_URL);
		if (dbUrl==null){
			dbUrl = "jdbc:postgresql://localhost:5432/download";
		}
		
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl(dbUrl);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		
		try {
			log.info("start loop ");
			
			// setup queue client
			DownloadQueue queueClient = new DownloadQueueClient(host, downloadQueueTubeName);
			DownloadDao downloadDao = new DownloadDao(dataSource);
			
			for (;;) {
				Thread.sleep(10000);
				
				String requestId = UUID.randomUUID().toString();
				DownloadRequest downloadRequest = new DownloadRequest(requestId);
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

				try {
					DownloadRequestInfo requestInfo = new DownloadRequestInfo(
							requestId, UUID.randomUUID().toString(), "rjstek", "rs@idgis.nl", "KML", download);
					downloadDao.createDownloadRequestInfo(requestInfo);
				} catch (SQLException e) {
					e.printStackTrace();
					log.debug("Exception while inserting requestInfo into db: " + e.getMessage());
				}
				
				queueClient.sendDownloadRequest(downloadRequest);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("end loop ");
		}


	}

}
