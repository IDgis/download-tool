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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(TestProcessor.class);
	
	@TestSubject
	private DownloadProcessor downloadProcessor = new DownloadProcessor(System.getProperty("user.home"));

	@Mock
	private DownloadQueue queueClientMock;
	@Mock(type = MockType.DEFAULT, fieldName = "feedbackQueue")
	private FeedbackQueue feedbackQueueMock;
	@Mock(type = MockType.STRICT, fieldName = "errorFeedbackQueue")
	private FeedbackQueue errorFeedbackQueueMock;
	
	/**
	 * Test method for {@link nl.idgis.downloadtool.downloader.DownloadProcessor#performDownload(nl.idgis.downloadtool.domain.DownloadRequest)}.
	 */
	@Test
	public void testPerformDownload() {
		log.debug("testPerformDownload");
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
		DownloadRequest downloadRequest = new DownloadRequest("testrequest3");
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
		additionalData.setName("someData.txt");
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

	private DownloadRequest makeRequestStrooizout(String mimetype, String extension) {
		DownloadRequest downloadRequest = new DownloadRequest("testrequest2");
		Download download = new Download();
		download.setName("strooiroutes" + mimetype);
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("B2_Strooiroutes_preventief_Provincie");
		ft.setExtension(extension);
		ft.setServiceUrl("http://acc-services.geodataoverijssel.nl/geoserver/B22_wegen/wfs");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype(mimetype);
		download.setFt(ft);
		AdditionalData additionalDataLayer = new AdditionalData();
		additionalDataLayer.setName("strooiroutes.lyr");
		additionalDataLayer
				.setUrl("http://gisopenbaar.overijssel.nl/GeoPortal/MIS4GIS/lyr/strooiroutes%20provincie_arc.lyr");
		AdditionalData additionalDataMetadata = new AdditionalData();
		additionalDataMetadata.setName("metadata.xml");
		additionalDataMetadata.setUrl(
				"http://acc-metadata.geodataoverijssel.nl/metadata/dataset/cd349c2f-b2fe-4ed6-b2b9-a00639ebcebb.xml");
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalDataLayer);
		additionalDataList.add(additionalDataMetadata);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		downloadRequest.setConvertToMimetype(mimetype);
		return downloadRequest;
	}

	@Test
	public void testStrooiroutesKML() throws Exception {
		DownloadRequest downloadRequest = makeRequestStrooizout("KML", "kml");
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
	public void testStrooiroutesSHP() throws Exception {
		DownloadRequest downloadRequest = makeRequestStrooizout("SHAPE-ZIP", "zip");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		feedbackQueueMock.sendFeedback(anyObject(Feedback.class));
		replay(feedbackQueueMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
		verify(feedbackQueueMock);
	}

	/**
	 * Fill wfsMimetype with null to test correct behaviour in processor.
	 * @param extension
	 * @return
	 */
	private DownloadRequest makeRequestRayongrenzen(String mimetype, String extension) {
		DownloadRequest downloadRequest = new DownloadRequest("testrequest1");
		Download download = new Download();
		download.setName("Rayongrenzen" + "_" + extension);
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("B1_Rayongrenzen_eenheid_WK");
		ft.setExtension(extension);
		ft.setServiceUrl("http://test-services.geodataoverijssel.nl/geoserver/B14_bestuurlijke_grenzen/wfs");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype(mimetype);
		download.setFt(ft);
		AdditionalData additionalDataLayer = new AdditionalData();
		additionalDataLayer.setName("Rayongrenzen.lyr");
		additionalDataLayer
				.setUrl("http://gisopenbaar.overijssel.nl/GeoPortal/MIS4GIS/lyr/rayonwk_polygon.lyr");
		AdditionalData additionalDataMetadata = new AdditionalData();
		additionalDataMetadata.setName("metadata.xml");
		additionalDataMetadata.setUrl(
				"http://test-metadata.geodataoverijssel.nl/metadata/dataset/7e03c460-f8b1-4ada-97e7-1ad2dfe98be8.xml");
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalDataLayer);
		additionalDataList.add(additionalDataMetadata);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		return downloadRequest;
	}


	@Test
	public void testRayongrenzenGML3() throws Exception {
		DownloadRequest downloadRequest = makeRequestRayongrenzen("gml32", "gml");
		downloadRequest.setConvertToMimetype("gml32");
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
	public void testRayongrenzenDXF() throws Exception {
		DownloadRequest downloadRequest = makeRequestRayongrenzen("DXF-ZIP", "zip");
		downloadRequest.setConvertToMimetype("DXF-ZIP");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		feedbackQueueMock.sendFeedback(anyObject(Feedback.class));
		replay(feedbackQueueMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
		verify(feedbackQueueMock);
	}	
	
	/**
	 * Fill wfsMimetype with null to test correct behaviour in processor.
	 * @param extension
	 * @return
	 */
	private DownloadRequest makeRequestOppWater(String mimetype, String extension) {
		DownloadRequest downloadRequest = new DownloadRequest("testrequestB3OppWater");
		Download download = new Download();
		download.setName("B3OppWater" + "_" + extension);
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("B3_Oppervlaktewateren");
		ft.setExtension(extension);
		ft.setServiceUrl("http://test-services.geodataoverijssel.nl/geoserver/B35_waterlopen/wfs");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype(mimetype);
		download.setFt(ft);
		AdditionalData additionalDataLayer = new AdditionalData();
		additionalDataLayer.setName("B3_Oppervlaktewateren.lyr");
		additionalDataLayer
				.setUrl("http://gisopenbaar.overijssel.nl/GeoPortal/MIS4GIS/lyr/WRONG_polygon.lyr");
		AdditionalData additionalDataMetadata = new AdditionalData();
		additionalDataMetadata.setName("metadata.xml");
		additionalDataMetadata.setUrl(
				"http://test-metadata.geodataoverijssel.nl/metadata/dataset/12345.xml");
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalDataLayer);
		additionalDataList.add(additionalDataMetadata);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		return downloadRequest;
	}


	@Test
	public void testOppWaterGML3() throws Exception {
		DownloadRequest downloadRequest = makeRequestOppWater("gml32", "gml");
		downloadRequest.setConvertToMimetype("gml32");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		feedbackQueueMock.sendFeedback(anyObject(Feedback.class));
		replay(feedbackQueueMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
		verify(feedbackQueueMock);
	}


}
