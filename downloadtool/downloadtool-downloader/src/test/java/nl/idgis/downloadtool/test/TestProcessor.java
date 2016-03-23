/**
 * 
 */
package nl.idgis.downloadtool.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.downloader.DownloadProcessor;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.FeedbackQueue;

/**
 * @author Rob
 *
 */
@RunWith(EasyMockRunner.class)
public class TestProcessor  extends EasyMockSupport {

	@TestSubject
	private DownloadProcessor downloadProcessor = new DownloadProcessor(System.getProperty("user.dir"));

	@Mock
	private DownloadQueue queueClientMock;
	@Mock(type = MockType.DEFAULT, fieldName = "feedbackQueue")
	private FeedbackQueue feedbackQueueMock;
	@Mock(type = MockType.STRICT, fieldName = "errorFeedbackQueue")
	private FeedbackQueue errorFeedbackQueueMock;
	
	/*
	
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
*/
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
	 * @throws Exception 
	 */
	@Test
	public void testOK() throws Exception {
		DownloadRequest downloadRequest = new DownloadRequest("3.14159");
		Download download = new Download();
		download.setName("download");
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("Featuretype");
		ft.setExtension("kml");
		ft.setServiceUrl("http://httpbin.org/post");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype("KML");
		download.setFt(ft);
		AdditionalData additionalData = new AdditionalData();
		additionalData.setName("someData");
		additionalData.setExtension("txt");
		additionalData.setUrl("http://httpbin.org/get");
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalData);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		downloadRequest.setConvertToMimetype("KML");
		
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		feedbackQueueMock.sendFeedback(anyObject(Feedback.class));
		replay(feedbackQueueMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
		verify(feedbackQueueMock);
	}

	@Test
	public void testNOK() throws Exception {
		expect(queueClientMock.receiveDownloadRequest()).andReturn(null);
		queueClientMock.deleteDownloadRequest(null);
		replay(queueClientMock);
		
		errorFeedbackQueueMock.sendFeedback(anyObject(Feedback.class));
		replay(errorFeedbackQueueMock);
	
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
		verify(errorFeedbackQueueMock);
	}

}
