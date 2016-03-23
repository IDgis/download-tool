package nl.idgis.downloadtool.downloader;

import java.io.BufferedInputStream;
import java.io.File;
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
import nl.idgis.commons.cache.FileCache;
import nl.idgis.commons.cache.ZippedCache;
import nl.idgis.commons.convert.Convert;
import nl.idgis.commons.convert.ConverterFactory;
import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.FeedbackQueue;

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

	private static final String ZIPCACHEPATH = "ZIP_CACHE_PATH";
	
	DownloadQueue queueClient;
	FeedbackQueue feedbackQueue, errorFeedbackQueue;
	
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
    		 * and no further item can be put into it afterwards. 
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
		DownloadSource source = new DownloadWfs(ft);
		ConverterFactory converterFactory = new ConverterFactory();
		/*
		 * Open wfs stream
		 */
		BufferedInputStream srcStream = new BufferedInputStream(source.open());
		// test if at http 200 OK an exceptionreport is send instead of the expected content
		// testExceptionReport(fromStream);			
		
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
			feedbackQueue.sendFeedback(feedback);
		} catch (Exception e1) {
			log.debug("Exception: " + e1);
			e1.printStackTrace();
			feedback.setResultCode(e1.getMessage());
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
	
	public static void main(String... args){
		String path = System.getenv(ZIPCACHEPATH);
		if(path == null) {
			path = System.getProperty("user.dir");
		}
		
		log.info("start loop " + path);
		DownloadProcessor dlp = new DownloadProcessor(path);	
		
		for (int i = 0; i < 5; i++) {
			log.debug("download nr \t" + i);
			
	    	dlp.processDownloadRequest();
		}
        
		log.info("end loop ");
    	
    }

}

