package nl.idgis.downloadtool.queue;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;

/**
 * The feedbackClient implements a queue between Processor and FeedbackProvider.<br>
 * <br>
 * This implementation uses beanstalkd as queuing mechanism .<br>
 * Objects are serialized over beanstalk as json.<br>
 *  
 * @see nl.idgis.downloadtool.downloader.DownloadProcessor
 *   
 * @author Rob
 *
 */
public class FeedbackQueueClient implements FeedbackQueue {
	private static final Logger log = LoggerFactory.getLogger(FeedbackQueueClient.class);
	
	/**
	 * Collection to keep jobs between get from queue en delete from queue.<br>
	 * key is downloadrequest, value is job id.
	 */
	private final Map<DownloadRequest, String> jobs;
	private String beanstalkTubeName;
	
	//TODO remove when beanstalk is implemented
	Feedback feedback = null;
	
	/**
	 * Constructs a FeedbackQueueClient.<br>
	 * @param beanstalkTubeName the queue identifier
	 */
	public FeedbackQueueClient(String beanstalkTubeName) {
		super();
		this.beanstalkTubeName = beanstalkTubeName;
		jobs = new HashMap<DownloadRequest,String>();
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#sendFeedback(nl.idgis.downloadtool.domain.Feedback)
	 */
	@Override
	public void sendFeedback(Feedback feedback){
		log.debug("send on " + this.beanstalkTubeName + " : " + feedback);
		// TODO beanstalk code
		this.feedback = feedback;
	}

	@Override
	public Feedback receiveFeedback() {
		// TODO beanstalk code
		// PSEUDO-CODE
		// get from feedback queue
		Feedback feedback = this.feedback;
		// delete from feedback queue
		this.feedback = null;
		log.debug("receive on " + this.beanstalkTubeName + " : " + feedback);
		return feedback;
	}
}
