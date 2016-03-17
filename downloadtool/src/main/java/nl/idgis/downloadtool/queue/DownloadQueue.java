package nl.idgis.downloadtool.queue;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;

/**
 * The DownloadQueue describes transferring downloadrequests and feedback.<br>
 * Three queues are used:<br>
 * 1. request: sending and receiving downloadrequests<br>
 * 2. feedback: sending and receiving feedback in case the download succeeds<br>
 * 3. feedbackerror: sending and receiving feedback in case the download fails<br>
 * <br>
 * 
 * @author Rob
 *
 */
public interface DownloadQueue {

	/**
	 * Put a download description in the request queue.
	 * @param downloadRequest
	 */
	void putDownloadRequest(DownloadRequest downloadRequest);

	/**
	 * Get a download description from the request queue. 
	 * @return DownloadRequest
	 */
	DownloadRequest getDownloadRequest();

	/**
	 * The download has ended and can be removed from the request queue.<br>
	 * The parameter is the same as received from the getDownloadRequest() method.
	 * @param downloadRequest
	 * @see getDownloadRequest()
	 */
	void deleteDownloadRequest(DownloadRequest downloadRequest);

	/**
	 * Send to feedback success queue.
	 * @param feedback
	 */
	void sendFeedback(Feedback feedback);

	/**
	 * Send to feedback error queue.
	 * @param feedback
	 */
	void sendErrorFeedback(Feedback feedback);

	/**
	 * Receive from the feedback queue.<br>
	 * Whether this is the feedback success or feedback error queue must be set by means of configuration.<br>
	 * It is assumed after this call the feedback is removed from the queue as well. 
	 * @return Feedback
	 */
	Feedback getFeedback();
	
}