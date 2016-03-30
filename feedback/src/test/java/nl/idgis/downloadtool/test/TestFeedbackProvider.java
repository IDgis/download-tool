/**
 * 
 */
package nl.idgis.downloadtool.test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.feedback.FeedbackProvider;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * @author Rob
 *
 */
public class TestFeedbackProvider   extends EasyMockSupport{
	private static final Logger log = LoggerFactory.getLogger(TestFeedbackProvider.class);
	
	private FeedbackQueue feedbackQueueMock;
	private DownloadDao downloadDaoMock;
	
	private String requestId;
	private Feedback feedback ;
	private DownloadResultInfo downloadResultInfo;
	private DownloadRequestInfo downloadRequestInfo;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		requestId = "1234";
		feedback = new Feedback(requestId);
		downloadResultInfo = new DownloadResultInfo(feedback);
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
		downloadRequestInfo = 
			new DownloadRequestInfo(requestId, "uuid", "user", "user@email.nl", "KML", download) ;
	}

	@Test
	public void test() throws Exception {
		feedbackQueueMock = mock(FeedbackQueue.class);
		expect(feedbackQueueMock.receiveFeedback()).andReturn(feedback);
		feedbackQueueMock.deleteFeedback(feedback);
		replay(feedbackQueueMock);

		downloadDaoMock = mock(DownloadDao.class);
		downloadDaoMock.createDownloadResultInfo(anyObject(DownloadResultInfo.class));
		expect(downloadDaoMock.readDownloadRequestInfo(requestId)).andReturn(downloadRequestInfo);
		replay(downloadDaoMock);
		
		FeedbackProvider fbp = new FeedbackProvider(feedbackQueueMock, downloadDaoMock);
		fbp.setSmtpHost("localhost");
		fbp.setSmtpPort(25);
		fbp.setFromAddress("mail@idgis.nl");
		fbp.setMsgTemplate("There is a downloadlink available for ${username} concerning ${featuretype}");
		fbp.setSubjectTemplate("Geoportaal downloader: ${featuretype} is available for download");

		fbp.processFeedback();
		
		verifyAll();
	}

}
