package nl.idgis.downloadtool.queue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;

/**
 * The queueClient implements a queue between Processor, DownloadRequestController and FeedbackProvider.<br>
 * Three queues are used:<br>
 * 1. receiving downloadrequests<br>
 * 2. sending feedback in case the download succeeds<br>
 * 3. sending feedback in case the download fails<br>
 * <br>
 * This implementation uses beanstalkd as queuing mechanism .<br>
 * Objects are serialized over beanstalk as json.<br>
 *  
 * @see nl.idgis.downloadtool.downloader.DownloadProcessor
 *   
 * @author Rob
 *
 */
public class QueueClient implements DownloadQueue {
	private static final Logger log = LoggerFactory.getLogger(QueueClient.class);
	
	/**
	 * Collection to keep jobs between get from queue en delete from queue.<br>
	 * key is downloadrequest, value is job id.
	 */
	private final Map<DownloadRequest, String> jobs;
	
	public QueueClient() {
		super();
		jobs = new HashMap<DownloadRequest,String>();
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#getDownloadRequest()
	 */
	@Override
	public DownloadRequest getDownloadRequest(){
		log.debug("get downloadrequest");
		// TODO replace with beanstalk code
		String uuid = UUID.randomUUID().toString();
		DownloadRequest downloadRequest = new DownloadRequest(uuid);
		String jobId = UUID.randomUUID().toString();
		
		jobs.put(downloadRequest, jobId);
		
		return downloadRequest;
	}
	
	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#deleteDownloadRequest(nl.idgis.downloadtool.domain.DownloadRequest)
	 */
	@Override
	public void deleteDownloadRequest(DownloadRequest downloadRequest){
		log.debug("delete downloadrequest job " + jobs.get(downloadRequest));
		String jobId = jobs.remove(downloadRequest); 
		// TODO beanstalk code
	}
	
	
	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#sendFeedback(nl.idgis.downloadtool.domain.Feedback)
	 */
	@Override
	public void sendFeedback(Feedback feedback){
		log.debug("feedback: " + feedback);
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#sendErrorFeedback(nl.idgis.downloadtool.domain.Feedback)
	 */
	@Override
	public void sendErrorFeedback(Feedback feedback){
		log.debug("error feedback: " + feedback);
	}

	@Override
	public void putDownloadRequest(DownloadRequest downloadRequest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Feedback getFeedback() {
		Feedback feedback = null;
		// TODO Auto-generated method stub
		// PSEUDO-CODE
		// get from feedback queue
		// delete from feedback queue
		return feedback;
	}
}
