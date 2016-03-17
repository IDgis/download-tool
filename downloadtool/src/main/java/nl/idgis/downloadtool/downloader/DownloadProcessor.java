package nl.idgis.downloadtool.downloader;

import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.QueueClient;

/**
 * The DownloadProcessor receives a DownloadRequest bean, performs downloads and sends a Feedback bean.<br>
 * 
 * The result of all downloads is packaged into a single zip file.<br>
 * Every single download has its own name and extension.<br>
 * Two types of downloads:<br>
 * 1. from a WFS with optional conversion (GML -> SHP, GML -> KML, ...)<br>
 * 2. additional data without conversion (metadata.xml, stylesheet, pdf, jpg, ...)<br>
 *
 * @author Rob
 *
 */
public class DownloadProcessor {
	private static final Logger log = LoggerFactory.getLogger(DownloadProcessor.class);
	
	// TODO autowire
	DownloadQueue queueClient;
	
    public DownloadProcessor() {
        super();
        queueClient = new QueueClient();
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
    	
    	if ((new Random().nextInt(10) % 3) == 0){
    		throw  new IllegalArgumentException("the wfs url could not be parsed!");
    	}
    }

    private void sleep(int milli) {
		try {
			Thread.sleep(milli);
		} catch (InterruptedException e) {
		}
	}

	public static void main(String... args){
        /*
         * PSEUDO-CODE
         * forever {
         *    lees uit queue
         *       downloadRequest Bean = requestQueue.reserveJob();
         *    initialize downloads and converters and packager   
         *       performDownload(downloadRequest Bean);
         *    put feedback into queue (OK queue or NOK queue depending on outcome)
         *       feedbackQueue.put(feedback Bean);
         *    end queue job
         *       requestQueue.deleteJob();   
         * }
         */
		DownloadProcessor dlp = new DownloadProcessor();
		log.info("start loop ");
		
		for (int i = 0; i < 5; i++) {
			log.debug("download nr \t" + i);
			
	    	DownloadRequest downloadRequest = dlp.queueClient.getDownloadRequest();
	    	Feedback feedback = new Feedback(downloadRequest.getRequestId());
	    	
	    	try {
				dlp.performDownload(downloadRequest);
				feedback.setResultCode("OK");
				dlp.queueClient.sendFeedback(feedback);
			} catch (Exception e1) {
				e1.printStackTrace();
				feedback.setResultCode(e1.getMessage());
				dlp.queueClient.sendErrorFeedback(feedback);
			}
	        
	        dlp.queueClient.deleteDownloadRequest(downloadRequest);
		}
        
		log.info("end loop ");
    	
    }
}

