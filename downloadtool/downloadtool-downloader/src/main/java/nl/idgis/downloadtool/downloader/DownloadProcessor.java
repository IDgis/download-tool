package nl.idgis.downloadtool.downloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.commons.cache.Cache;
import nl.idgis.commons.cache.ZippedCache;
import nl.idgis.commons.convert.Convert;
import nl.idgis.commons.convert.ConverterFactory;
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
     * Perform downloads using parameter as input, then send feedback.<br>
     * @param downloadRequest contains all information concerning the downloads requested.
     * @return featurecount
     */
    public long performDownload(DownloadRequest downloadRequest) throws Exception {  	
    	long featureCount = -1;
    	
    	if (downloadRequest == null){
    		throw new IllegalArgumentException("downloadrequest is null");
    	} else {
    		Download download = downloadRequest.getDownload();
    		if (download == null)
        		throw new IllegalArgumentException("downloadrequest does not contain valid downloads");
        	
    		
    		/*
    		 * Download Wfs data and put into temporary cache.
    		 * Reason for a temp cache is that the converter closes the cache stream
    		 * and no further item (AdditionalData) can be put into it afterwards. 
    		 */
    		WfsFeatureType ft = download.getFt();
    		// temporary cache for the wfs data
    		Cache tempCache = new ZippedCache(cachePath, download.getName() + "_temp.zip");
    		tempCache.rmCache(); // make sure last cache is deleted before use
    		tempCache = new ZippedCache(cachePath, download.getName() + "_temp.zip");
    		featureCount = downloadConvertWfsData(tempCache, ft, downloadRequest.getConvertToMimetype());
			if (ft.getWfsMimetype().equalsIgnoreCase(downloadRequest.getConvertToMimetype())){
				log.debug("Converted #bytes: "+ featureCount);
			} else {
				log.debug("Converted #features: "+ featureCount);
			}
    		tempCache.close();
    		
			Cache downloadCache = new ZippedCache(cachePath, download.getName() + ".zip");
			downloadCache.rmCache();// make sure last cache is deleted before use
			downloadCache = new ZippedCache(cachePath, download.getName() + ".zip");
			/*
			 * Copy Wfs data to downloadCache
			 */
			tempCache = new ZippedCache(cachePath, download.getName() + "_temp.zip");
			List<String> items = tempCache.getItemList();
			for (String item : items) {
				InputStream is = tempCache.readItem(item);
				OutputStream  downloadCacheOutputStream = downloadCache.writeItem(item);
				// perform the actual copying
				copyStreams(is, downloadCacheOutputStream);
				is.close();
				log.debug("Wfs data to downloadCache: " + item);
			}

			/*
			 * Download additional data items and put in downloadCache
			 */
			OutputStream  downloadCacheOutputStream = null;
    		List<AdditionalData> additionalData = download.getAdditionalData();
    		for (AdditionalData data : additionalData) {
    			log.debug("Additional item to downloadCache: " + data.getName() + "." + data.getExtension());
    			downloadCacheOutputStream = downloadAdditionalData(downloadCache, data);
			}
    		
    		// close downloadcache stream
    		if (downloadCacheOutputStream != null)
    			downloadCacheOutputStream.close();
    		// close downloadcache
    		downloadCache.close();
    	}
    	return featureCount;
    }

	private long downloadConvertWfsData(Cache downloadCache, WfsFeatureType ft, String convertToMimetype)
			throws MalformedURLException, UnsupportedEncodingException, URISyntaxException, IOException {
		
		long featureCount = -1;
		OutputStream dstStream = downloadCache.writeItem(ft.getName() + "." + ft.getExtension());
		if (ft.getWfsMimetype() == null || ft.getWfsMimetype().isEmpty()){
			ft.setWfsMimetype(convertToMimetype);
		}
		DownloadSource source = new DownloadWfs(ft);
		ConverterFactory converterFactory = new ConverterFactory();
		/*
		 * Open wfs stream
		 */
		BufferedInputStream srcStream = new BufferedInputStream(source.open());
		// test if at http 200 OK an exceptionreport is send instead of the expected content
		testExceptionReport(srcStream);			
		
		/*
		 * Start download and conversion from Wfs 
		 */
		Convert converter = converterFactory.getConverter(
				ft.getWfsMimetype().toLowerCase(), 
				convertToMimetype.toLowerCase()
			);
		log.debug("Start conversion: from " + converter.getInputMimeType() + " to " + converter.getOutputMimeType());
		try {
			featureCount = converter.convert(srcStream, dstStream);
		} catch (Exception e) {
			throw new IOException(e);
		}
		srcStream.close();
		return featureCount;
	}

	private OutputStream downloadAdditionalData(Cache downloadCache, AdditionalData data)
			throws MalformedURLException, UnsupportedEncodingException, URISyntaxException, Exception, IOException {
		DownloadSource source = new DownloadFile(data);
		BufferedInputStream srcStream = new BufferedInputStream(source.open());
		OutputStream dstStream = downloadCache.writeItem(data.getName() + "." + data.getExtension());
		long byteCount = copyStreams(srcStream, dstStream) ;
		log.debug("Converted #bytes: "+ byteCount);
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
			long featureCount = performDownload(downloadRequest);
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
			throws Exception {
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
