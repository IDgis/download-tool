package nl.idgis.downloadtool.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.downloader.DownloadWfs;

public class TestDownloadWfs {
	private static final String URL = "http://httpbin.org/post";
	private static final Logger log = LoggerFactory.getLogger(TestDownloadFile.class);
	DownloadWfs downloadSource;
	

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testPost() {
		WfsFeatureType wfsFeatureType = new WfsFeatureType();
		wfsFeatureType.setCrs("EPSG:28992");
		wfsFeatureType.setName("ProtectedSites");
		wfsFeatureType.setExtension("kml");
		wfsFeatureType.setServiceUrl(URL);
		wfsFeatureType.setServiceVersion("2.0.0");
		wfsFeatureType.setWfsMimetype("KML");
		
		
		try {
			downloadSource = new DownloadWfs(wfsFeatureType);
			InputStream is = downloadSource.open();
			StringBuilder inputStringBuilder = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = bufferedReader.readLine();
			while (line != null) {
				inputStringBuilder.append(line);
				inputStringBuilder.append('\n');
				line = bufferedReader.readLine();
			}
			String result = inputStringBuilder.toString();
			if (log.isDebugEnabled()) {
				log.debug("---------- Http InputStream ----------");
				log.debug(result);
				log.debug("--------------------------------------");
			}
			assertTrue("Downloadresponse expected!", result.indexOf("http://schemas.opengis.net/wfs/2.0/wfs.xsd") > -1);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception not expected!");
		}
	}

	@Test
	public void testWfs(){
		// test with Inspire acc AreaManagement
		WfsFeatureType wfsFeatureType = new WfsFeatureType();
		wfsFeatureType.setCrs("EPSG:28992");
		wfsFeatureType.setName("ManagementRestrictionOrRegulationZone");
		wfsFeatureType.setExtension("gml");
		wfsFeatureType.setNamespacePrefix("am");
		wfsFeatureType.setNamespaceUri("http://inspire.ec.europa.eu/schemas/am/3.0rc2");
//		wfsFeatureType.setFilterExpression("<ogc:PropertyIsEqualTo><ogc:PropertyName>inspire_id_namespace</ogc:PropertyName><ogc:Literal>NL.glvvdn</ogc:Literal></ogc:PropertyIsEqualTo>");
		wfsFeatureType.setServiceUrl("http://acc-services.inspire-provincies.nl/AreaManagement/services/download_AM?");
		wfsFeatureType.setServiceVersion("2.0.0");
		wfsFeatureType.setWfsMimetype("application/gml+xml; version=3.2");

		try {
			downloadSource = new DownloadWfs(wfsFeatureType, 1); // maxfeatures = 1
			InputStream is = downloadSource.open();
			StringBuilder inputStringBuilder = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = bufferedReader.readLine();
			while (line != null) {
				inputStringBuilder.append(line);
				inputStringBuilder.append('\n');
				line = bufferedReader.readLine();
			}
			String result = inputStringBuilder.toString();
			if (log.isDebugEnabled()) {
				log.debug("---------- Http InputStream ----------");
				log.debug(result);
				log.debug("--------------------------------------");
			}
			assertTrue("FeatureCollection expected!", result.indexOf("</wfs:FeatureCollection>") > -1);
			assertTrue("Element am:ManagementRestrictionOrRegulationZone expected!", result.indexOf("</am:ManagementRestrictionOrRegulationZone>") > -1);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception not expected!");
		}
	}
}
