package nl.idgis.downloadtool.downloader;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;
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
	
	// TODO autowire
	DownloadQueue queueClient;
	FeedbackQueue feedbackQueue, errorFeedbackQueue;
	
    public DownloadProcessor() {
        super();
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
     */
    public void performDownload(DownloadRequest downloadRequest) throws Exception {
        
        /*
         * PSEUDO-CODE
         * maak een wfs downloader met property values in downloadRequest Bean
         * haal data op van de wfs en cache deze
         *     putCache(downloadFromWfs(Wfs.URI));
         * voor alle gevraagde mimetypes converteer cache en stop in zip packager
         * for mimetype : mimetypes
         *    putToPackager(convert(mimetype, getCache()));
         * additional download(s)
         * for download : downloads 
         *    putToPackager(downloadFromSource(download.URI));
         */
    	
    	
    	
    	
    	sleep(new Random().nextInt(2000));
    	
    	if (downloadRequest == null){
    		throw new IllegalArgumentException("downloadrequest is null");
    	}
    }

    private void sleep(int milli) {
		try {
			Thread.sleep(milli);
		} catch (InterruptedException e) {
		}
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
			feedbackQueue.sendFeedback(feedback);
		} catch (Exception e1) {
			log.debug(e1.getMessage());
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

	public static void main(String... args){
		DownloadProcessor dlp = new DownloadProcessor();	
		log.info("start loop ");
		
		for (int i = 0; i < 5; i++) {
			log.debug("download nr \t" + i);
			
	    	dlp.processDownloadRequest();
		}
        
		log.info("end loop ");
    	
    }

}

