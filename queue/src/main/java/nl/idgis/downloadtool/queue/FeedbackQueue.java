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
	 * Send to feedback queue.
	 * @param feedback
	 */
	void sendFeedback(Feedback feedback);

	/**
	 * Receive from the feedback queue.<br>
	 * Whether this is a feedback success or feedback error queue must be set by means of configuration.<br>
	 * @return Feedback
	 */
	Feedback receiveFeedback();
	
	/**
	 * The feedback has processed and can be removed from the request queue.<br>
	 * The parameter is the same object as received from the receiveFeedback() method.
	 * @param feedback
	 * @throws Exception when the feedback was already deleted from the queue
	 * @see #receiveFeedback()
	 */
	void deleteFeedback(Feedback feedback) throws Exception;

}