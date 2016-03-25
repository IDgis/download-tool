/**
 * 
 */
package nl.idgis.downloadtool.feedback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * @author Rob
 *
 */
public class FeedbackProvider {
	private static final Logger log = LoggerFactory.getLogger(FeedbackProvider.class);

	private static final String BEANSTALK_HOST = "BEANSTALK_HOST";
	private static final String BEANSTALK_FEEDBACK_QUEUE = "BEANSTALK_FEEDBACK_QUEUE";
	
	private FeedbackQueue feedbackQueue;

	public FeedbackProvider(FeedbackQueue feedbackQueue) {
		super();
		this.feedbackQueue = feedbackQueue;
	}

    public void setFeedbackQueue(FeedbackQueue queueClient){
    	this.feedbackQueue = queueClient;
    }
    
	private void processFeedback() throws Exception {
		/*
		 * get feedback from queue
		 */
		Feedback feedback = feedbackQueue.receiveFeedback();
		log.debug("Feedback received: " + feedback);

		/* 
		 * assemble email from:
		 *   - database data using feedback.getRequestId()
		 *   - email templates from configuration 
		 */
		// TODO assemble email
		
		/*
		 * send email
		 */
		//TODO send email
		
		/*
		 * delete feedback item from queue
		 */
		feedbackQueue.deleteFeedback(feedback);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String host = System.getenv(BEANSTALK_HOST);
		if(host == null) {
			host = "localhost";
		}
		String feedbackTubeName = System.getenv(BEANSTALK_FEEDBACK_QUEUE);
		if(feedbackTubeName == null) {
			feedbackTubeName = "feedbackTube";
		}
		
		try {
			log.info("start loop ");
			
			// setup queue client
			FeedbackQueueClient feedbackQueueClient = new FeedbackQueueClient(host, feedbackTubeName); 
			FeedbackProvider fbp = new FeedbackProvider(feedbackQueueClient);
			
			for (;;) {
				log.debug("processFeedback");
				fbp.processFeedback();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("end loop ");
		}

	}

}
