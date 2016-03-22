package nl.idgis.downloadtool.queue;

import nl.idgis.downloadtool.domain.Feedback;

/**
 * The FeedbackQueue is used for transferring feedback.<br>
 * <br>
 * 
 * @author Rob
 *
 */
public interface FeedbackQueue {

	/**
	 * Send to feedback success queue.
	 * @param feedback
	 */
	void sendFeedback(Feedback feedback);

	/**
	 * Receive from the feedback queue.<br>
	 * Whether this is the feedback success or feedback error queue must be set by means of configuration.<br>
	 * It is assumed after this call the received feedback is removed from the queue as well. 
	 * @return Feedback
	 */
	Feedback receiveFeedback();
	
}