/**
 * 
 */
package nl.idgis.downloadtool.test;

import static org.junit.Assert.*;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.downloader.DownloadProcessor;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * @author Rob
 *
 */
public class TestProcessor  extends EasyMockSupport {

	DownloadProcessor downloadProcessor;
	DownloadQueue queueClientMock;
	FeedbackQueue feedbackQueueMock, errorFeedbackQueueMock;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		downloadProcessor = new DownloadProcessor();
		// TODO mock when beanstalk code is in place
		queueClientMock = new DownloadQueueClient("download");
		feedbackQueueMock = new FeedbackQueueClient("feedbackOK");
		errorFeedbackQueueMock = new FeedbackQueueClient("feedbackNOK");
		
		downloadProcessor.setDownloadQueueClient(queueClientMock);
		downloadProcessor.setFeedbackQueue(feedbackQueueMock);
		downloadProcessor.setErrorFeedbackQueue(errorFeedbackQueueMock);
	}

	/**
	 * Test method for {@link nl.idgis.downloadtool.downloader.DownloadProcessor#performDownload(nl.idgis.downloadtool.domain.DownloadRequest)}.
	 */
	@Test
	public void testPerformDownload() {
		try {
			downloadProcessor.performDownload(null);
			fail("Exception expected");
		} catch (Exception e) {
		}
	}

	/**
	 * Test method for {@link nl.idgis.downloadtool.downloader.DownloadProcessor#main(java.lang.String[])}.
	 */
	@Test
	public void testOK() {
		queueClientMock.sendDownloadRequest(new DownloadRequest("3.14159"));
		
		downloadProcessor.processDownloadRequest();
		
		assertNotNull("expected feedback on OK queue", feedbackQueueMock.receiveFeedback());
		assertNull("expected no feedback on NOK queue", errorFeedbackQueueMock.receiveFeedback());
		
	}

	@Test
	public void testNOK() {
		queueClientMock.sendDownloadRequest(null);
		
		downloadProcessor.processDownloadRequest();
		
		assertNotNull("expected feedback on NOK queue", errorFeedbackQueueMock.receiveFeedback());
		assertNull("expected no feedback on OK queue", feedbackQueueMock.receiveFeedback());
	}

}
