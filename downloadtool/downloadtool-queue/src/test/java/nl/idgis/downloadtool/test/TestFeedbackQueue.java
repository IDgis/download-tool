/**
 * 
 */
package nl.idgis.downloadtool.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * @author Rob
 *
 */
public class TestFeedbackQueue {

	FeedbackQueue feedbackQueue;
	Feedback feedback ;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		feedbackQueue = new FeedbackQueueClient("feedback");
	}

	@Test
	public void test() {
		feedback = new Feedback("requestid");
		feedbackQueue.sendFeedback(feedback);
		Feedback receivedFeedback = feedbackQueue.receiveFeedback();
		assertTrue("received feedback should be equal to sent feedback", feedback.equals(receivedFeedback));
	}

	@Test
	public void testEmptyQueue() {
		feedback = new Feedback("requestid");
		feedbackQueue.sendFeedback(feedback);
		Feedback receivedFeedback = feedbackQueue.receiveFeedback();
		receivedFeedback = feedbackQueue.receiveFeedback();
		assertTrue("receive twice after sending should give null", receivedFeedback == null);
	}

}
