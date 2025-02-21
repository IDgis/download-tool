package nl.idgis.downloadtool.downloader;

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
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.downloader.DownloadProcessor;
import nl.idgis.downloadtool.queue.DownloadQueue;

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
	public void testOK_WFS() throws Exception {
		DownloadRequest downloadRequest = new DownloadRequest("testrequestOkWfs");
		Download download = new Download();
		download.setName("download");
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("Featuretype");
		ft.setExtension("kml");
		ft.setServiceUrl("http://httpbin.org/get");
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
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	@Test
	public void testNOK_WFS() throws Exception {
		DownloadRequest downloadRequest = new DownloadRequest("testrequestNokWfs");
		Download download = new Download();
		download.setName("download");
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("Featuretype");
		ft.setExtension("kml");
		ft.setServiceUrl("http://httpbin.org/post"); // wfs request is GET
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
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	@Test
	public void testNOK_DATA() throws Exception {
		DownloadRequest downloadRequest = new DownloadRequest("testrequestNokData");
		Download download = new Download();
		download.setName("download");
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("Featuretype");
		ft.setExtension("kml");
		ft.setServiceUrl("http://httpbin.org/get");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype("KML");
		download.setFt(ft);
		AdditionalData additionalData = new AdditionalData();
		additionalData.setName("someData.txt");
		additionalData.setUrl("http://httpbin.org/post"); //Data is GET request
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalData);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		downloadRequest.setConvertToMimetype("KML");
		
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	@Test
	public void testNOK() throws Exception {
		expect(queueClientMock.receiveDownloadRequest()).andReturn(null);
		queueClientMock.deleteDownloadRequest(null);
		replay(queueClientMock);
	
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	private DownloadRequest makeRequestBebouwdeKommen(String mimetype, String extension) {
		String name = "bebouwdekommen" + "_" + mimetype;
		DownloadRequest downloadRequest = new DownloadRequest(name);
		Download download = new Download();
		download.setName(name);
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("B0_Bebouwde_kommen_in_Overijssel");
		ft.setExtension(extension);
		ft.setServiceUrl("https://acc-services.geodataoverijssel.nl/geoserver/B04_stedelijk_gebied/wfs");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype(mimetype);
		download.setFt(ft);
		AdditionalData additionalDataLayer = new AdditionalData();
		additionalDataLayer.setName("bebouwdekommen.lyr");
		additionalDataLayer
				.setUrl("https://acc-metadata.geodataoverijssel.nl/attachment/68597/kernen_polygon.lyr");
		AdditionalData additionalDataMetadata = new AdditionalData();
		additionalDataMetadata.setName("metadata.xml");
		additionalDataMetadata.setUrl(
				"https://acc-metadata.geodataoverijssel.nl/metadata/dataset/f8c9f8c6-1f34-4951-a91f-32ab436a927e.xml");
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalDataLayer);
		additionalDataList.add(additionalDataMetadata);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		downloadRequest.setConvertToMimetype(mimetype);
		return downloadRequest;
	}

	@Test
	public void testBebouwdeKommenKML() throws Exception {
		DownloadRequest downloadRequest = makeRequestBebouwdeKommen("KML", "kml");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	@Test
	public void testBebouwdeKommenSHP() throws Exception {
		DownloadRequest downloadRequest = makeRequestBebouwdeKommen("SHAPE-ZIP", "zip");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	/**
	 * Fill wfsMimetype with null to test correct behaviour in processor.
	 * @param extension
	 * @return
	 */
	private DownloadRequest makeRequestRayongrenzen(String mimetype, String extension) {
		String name = "rayongrenzen" + "_" + mimetype;
		DownloadRequest downloadRequest = new DownloadRequest(name);
		Download download = new Download();
		download.setName(name);
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("B1_Rayongrenzen_eenheid_WK");
		ft.setExtension(extension);
		ft.setServiceUrl("https://acc-services.geodataoverijssel.nl/geoserver/B14_bestuurlijke_grenzen/wfs");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype(mimetype);
		download.setFt(ft);
		AdditionalData additionalDataLayer = new AdditionalData();
		additionalDataLayer.setName("rayongrenzen.lyr");
		additionalDataLayer
				.setUrl("https://acc-metadata.geodataoverijssel.nl/attachment/69026/rayonwk_polygon.lyr");
		AdditionalData additionalDataMetadata = new AdditionalData();
		additionalDataMetadata.setName("metadata.xml");
		additionalDataMetadata.setUrl(
				"https://acc-metadata.geodataoverijssel.nl/metadata/dataset/7e03c460-f8b1-4ada-97e7-1ad2dfe98be8.xml");
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
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}

	@Test
	public void testRayongrenzenDXF() throws Exception {
		DownloadRequest downloadRequest = makeRequestRayongrenzen("DXF", "zip");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}	
	
	/**
	 * Fill wfsMimetype with null to test correct behaviour in processor.
	 * @param extension
	 * @return
	 */
	private DownloadRequest makeRequestGebiedskenmerken(String mimetype, String extension) {
		String name = "gebiedskenmerken" + "_" + mimetype;
		DownloadRequest downloadRequest = new DownloadRequest(name);
		Download download = new Download();
		download.setName(name);
		WfsFeatureType ft = new WfsFeatureType();
		ft.setCrs("EPSG:28992");
		ft.setName("B0_Gebiedskenmerken_Stedelijke_laag");
		ft.setExtension(extension);
		ft.setServiceUrl("https://acc-services.geodataoverijssel.nl/geoserver/B04_stedelijk_gebied/wfs");
		ft.setServiceVersion("2.0.0");
		ft.setWfsMimetype(mimetype);
		download.setFt(ft);
		AdditionalData additionalDataLayer = new AdditionalData();
		additionalDataLayer.setName("gebiedskenmerken.lyr");
		additionalDataLayer
				.setUrl("https://acc-metadata.geodataoverijssel.nl/attachment/72245/stedelijke_laag_polygon.lyr");
		AdditionalData additionalDataMetadata = new AdditionalData();
		additionalDataMetadata.setName("metadata.xml");
		additionalDataMetadata.setUrl(
				"https://acc-metadata.geodataoverijssel.nl/metadata/dataset/c9bad93e-d8b9-42b6-b328-c17b7950bbe2.xml");
		List<AdditionalData> additionalDataList = new ArrayList<AdditionalData>();
		additionalDataList.add(additionalDataLayer);
		additionalDataList.add(additionalDataMetadata);
		download.setAdditionalData(additionalDataList);
		downloadRequest.setDownload(download);
		return downloadRequest;
	}


	@Test
	public void testGebiedskenmerkenGML3() throws Exception {
		DownloadRequest downloadRequest = makeRequestGebiedskenmerken("gml32", "gml");
		expect(queueClientMock.receiveDownloadRequest()).andReturn(downloadRequest);
		queueClientMock.deleteDownloadRequest(downloadRequest);
		replay(queueClientMock);
		
		downloadProcessor.processDownloadRequest();
		
		verify(queueClientMock);
	}
}
