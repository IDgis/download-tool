package nl.idgis.downloadtool.downloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.commons.cache.Cache;
import nl.idgis.commons.cache.ZippedCache;
import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * The DownloadProcessor receives a DownloadRequest bean, performs downloads and sends a Feedback bean.<br>
 * 
 * The result of all downloads is packaged into a single zip file.<br>
 * Every single download has its own name and extension.<br>
 * Two types of downloads:<br>
 * 1. from a WFS with optional conversion (GML2SHP, GML2KML, ...)<br>
 * 2. additional data without conversion (metadata.xml, stylesheet, pdf, jpg, ...)<br>
 *
 * @author Rob
 *
 */
public class DownloadProcessor {
	private static final Logger log = LoggerFactory.getLogger(DownloadProcessor.class);
	
	private static final int BUF_SIZE = 4096;
	
	private static final String BEANSTALK_HOST = "BEANSTALK_HOST";
	private static final String ZIPCACHEPATH = "ZIP_CACHEPATH";
	private static final String BEANSTALK_DOWNLOAD_QUEUE = "BEANSTALK_DOWNLOAD_QUEUE";
	private static final String BEANSTALK_FEEDBACKOK_QUEUE = "BEANSTALK_FEEDBACKOK_QUEUE";
	private static final String BEANSTALK_FEEDBACKERROR_QUEUE = "BEANSTALK_FEEDBACKERROR_QUEUE";
	
	private DownloadQueue queueClient;
	private FeedbackQueue feedbackQueue, errorFeedbackQueue;
	
	String cachePath;
	
    public DownloadProcessor(String cachePath) {
        super();
        this.cachePath = cachePath;
        log.debug("downloadpath: " + cachePath);
    }
    
    public void setDownloadQueueClient(DownloadQueue queueClient){
    	this.queueClient = queueClient;
    }
    
    public void setFeedbackQueue(FeedbackQueue queueClient){
    	this.feedbackQueue = queueClient;
    }
    
    public void setErrorFeedbackQueue(FeedbackQueue queueClient){
    	this.errorFeedbackQueue = queueClient;
    }
    
    /**
     * Perform downloads using downloadRequest as input.<br>
     * 1. a cache file is opened.<br>
     * 2. data from wfs is downloaded into the cache.<br>
     * 3. additional data is downloaded into the cache.<br>
     * 4. the cache is closed.<br>
     * @param downloadRequest contains all information concerning the downloads requested.
     */
    public void performDownload(DownloadRequest downloadRequest) throws Exception {  	
    	if (downloadRequest == null){
    		throw new IllegalArgumentException("downloadrequest is null");
    	} else {
    		Download download = downloadRequest.getDownload();
    		if (download == null)
        		throw new IllegalArgumentException("downloadrequest does not contain valid downloads");

    		Cache downloadCache = new ZippedCache(cachePath, download.getName() + ".zip");
    		// make sure last cache with the same name is deleted before use
    		downloadCache.rmCache();
    		downloadCache = new ZippedCache(cachePath, download.getName() + ".zip");
    		/*
    		 * Download Wfs data and put in downloadCache
    		 */
    		WfsFeatureType ft = download.getFt();
    		DownloadSource source = new DownloadWfs(ft);
    		OutputStream downloadCacheOutputStream = downloadData(source, downloadCache, ft.getName(), ft.getExtension()); 
			/*
			 * Download additional data items and put them in downloadCache
			 */
    		List<AdditionalData> additionalData = download.getAdditionalData();
    		for (AdditionalData data : additionalData) {
    			log.debug("Additional item to downloadCache: " + data.getName() + "." + data.getExtension());
    			source = new DownloadFile(data);
    			downloadCacheOutputStream = downloadData(source, downloadCache, data.getName(), data.getExtension());
			}
    		// close downloadcache stream after all downloads have finished
    		if (downloadCacheOutputStream != null)
    			downloadCacheOutputStream.close();
    		downloadCache.close();
    	}
    }
    
	/**
	 * Download data from a source and copy it into a cache.<br>
	 * Test for a WFS exceptionreport and raise an exception before starting the copying.
	 * @param source of the download data (delivers an InputStream which is closed in this method)
	 * @param downloadCache to write a new item to with content from source
	 * @param fileName name of the item in the cache
	 * @param fileExtension extension of the item in the cache
	 * @return OutputStream of the cache (which is not closed by this method)
	 * @throws IllegalArgumentException if an exceptionreport was received
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private OutputStream downloadData(DownloadSource source, Cache downloadCache, String fileName, String fileExtension)
			throws IllegalArgumentException, UnsupportedEncodingException, URISyntaxException, IOException {
		/*
		 * Open source stream
		 */
		BufferedInputStream srcStream = new BufferedInputStream(source.open());
		// test if at http 200 OK an exceptionreport is send instead of the expected content
		testExceptionReport(srcStream);
		/*
		 * Open destination stream
		 */
		OutputStream dstStream = downloadCache.writeItem(fileName + "." + fileExtension);
		/*
		 * Copy from source to destination
		 */
		long byteCount = copyStreams(srcStream, dstStream) ;
		log.debug("Data '" + fileName + "' #bytes: " + byteCount);
		srcStream.close();
		return dstStream;
	}


    /*
     * PSEUDO-CODE
     *    lees uit queue
     *       downloadRequest Bean = requestQueue.reserveJob();
     *    initialize downloads and converters and packager   
     *       performDownload(downloadRequest Bean);
     *    put feedback into queue (OK queue or NOK queue depending on outcome)
     *       feedbackQueue.put(feedback Bean);
     *    end queue job
     *       requestQueue.deleteJob();   
     */
	public void processDownloadRequest() {
		DownloadRequest downloadRequest = queueClient.receiveDownloadRequest();
		
		Feedback feedback = new Feedback(downloadRequest==null?null:downloadRequest.getRequestId());
		try {
			performDownload(downloadRequest);
			feedback.setResultCode("OK");
			log.debug("Feedback OK: " + feedback);
			feedbackQueue.sendFeedback(feedback);
		} catch (Exception e1) {
			e1.printStackTrace();
			feedback.setResultCode(e1.getMessage());
			log.debug("Feedback NOK: " + feedback);
			errorFeedbackQueue.sendFeedback(feedback);
		}
		
		try {
			queueClient.deleteDownloadRequest(downloadRequest);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private long copyStreams(InputStream is, OutputStream os)
			throws IOException {
		byte[] b = new byte[8192];
		int read;
		long total = 0;
		while ((read = is.read(b)) != -1) {
			os.write(b, 0, read);
			total += read;
		}
		return total;
	}

	/**
	 * Test if the stream contains an exception report instead of an expected feature collection.
	 * @param fromStream stream to test for content "ExceptionReport"
	 * @return true if stream was read and reset, false if the stream has to be reopened
	 * @throws IOException if stream could not be read
	 * @throws UnsupportedEncodingException
	 * @throws Exception containing an ExceptionReport if one occurred
	 */
	private boolean testExceptionReport(BufferedInputStream fromStream) throws IOException, UnsupportedEncodingException,
	IllegalArgumentException {
		int bytesRead = BUF_SIZE - 2; 
		boolean markSupported = fromStream.markSupported();
		if (markSupported){ 
			fromStream.mark(bytesRead);
			log.debug("Inputstream mark supported, test if ExceptionReport is send and reset stream");
		}else{
			log.debug("Inputstream mark/reset not supported, read stream for ExceptionReport and reopen stream");
		}
		byte[] b = new byte [bytesRead];
		fromStream.read(b);
		String s = new String(b, "UTF-8");
		// remove x00 bytes at the end
		s = s.substring(0, s.lastIndexOf(">") + 1);
		if (s.indexOf("ExceptionReport")>0){
			log.debug("found an ExceptionReport: ");
			throw new IllegalArgumentException("ExceptionReport: " + s);
		}else if (s.indexOf("FeatureCollection")>0){
			log.trace("found a FeatureCollection: ");
		}else{
			// do nothing, in future there may be KML or other formats read from the source
		}
		if (markSupported) 
			fromStream.reset();
		return markSupported;
	}

	
	public static void main(String... args){
		String path = System.getenv(ZIPCACHEPATH);
		if(path == null) {
			path = System.getProperty("user.home");
		}
		String host = System.getenv(BEANSTALK_HOST);
		if(host == null) {
			host = "localhost";
		}
		String downloadQueueTubeName = System.getenv(BEANSTALK_DOWNLOAD_QUEUE);
		if(downloadQueueTubeName == null) {
			downloadQueueTubeName = "downloadRequestTube";
		}
		String feedbackOkTubeName = System.getenv(BEANSTALK_FEEDBACKOK_QUEUE);
		if(feedbackOkTubeName == null) {
			feedbackOkTubeName = "feedbackOkTube";
		}
		String feedbackErrorTubeName = System.getenv(BEANSTALK_FEEDBACKERROR_QUEUE);
		if(feedbackErrorTubeName == null) {
			feedbackErrorTubeName = "feedbackErrorTube";
		}
		
		try {
			log.info("start loop " + path);
			DownloadProcessor dlp = new DownloadProcessor(path);
			// setup queue clients
			DownloadQueueClient downloadQueueClient = new DownloadQueueClient(host, downloadQueueTubeName);
			FeedbackQueueClient feedbackOkQueueClient = new FeedbackQueueClient(host, feedbackOkTubeName); 
			FeedbackQueueClient feedbackErrorQueueClient = new FeedbackQueueClient(host, feedbackErrorTubeName); 
			
			dlp.setDownloadQueueClient(downloadQueueClient);
			dlp.setFeedbackQueue(feedbackOkQueueClient);
			dlp.setErrorFeedbackQueue(feedbackErrorQueueClient);
			
			for (;;) {
				log.debug("processDownloadRequest");
				dlp.processDownloadRequest();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("end loop ");
		}
        
    }

}
