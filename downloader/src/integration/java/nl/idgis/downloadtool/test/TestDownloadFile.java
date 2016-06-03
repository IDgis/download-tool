package nl.idgis.downloadtool.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.downloader.DownloadFile;
import nl.idgis.downloadtool.downloader.DownloadSource;

public class TestDownloadFile {
	private static final String URL = "http://httpbin.org/get";
	private static final Logger log = LoggerFactory.getLogger(TestDownloadFile.class);
	DownloadSource downloadSource;
	
	@Before
	public void setUp() throws Exception {
		AdditionalData additionalData  = new AdditionalData();
		additionalData.setName("someFile.someExt");
		additionalData.setUrl(URL);
		downloadSource = new DownloadFile(additionalData);
	}
	
	@After
	public void TearDown(){
		try {
			downloadSource.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testOpen() {
		
		try {
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
			assertTrue("Downloadresponse expected!", result.indexOf(URL) > -1);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception not expected!");
		} 
	}

	@Test
	public void testClose() {
		try {
			InputStream is = downloadSource.open();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception not expected!");
		}
		try {
			downloadSource.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception not expected!");
		}
		try {
			downloadSource.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception not expected!");
		}
	}

	@Test
	public void testGetUri() {
		String s = downloadSource.getUri().toString();
		log.debug("Uri: " + s);
		assertTrue("", s.indexOf(URL) > -1);
	}

}
