package nl.idgis.downloadtool.test;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;

public class TestDownloadQueue {
	private static final Logger log = LoggerFactory.getLogger(TestDownloadQueue.class);
	DownloadQueue queueClient;
	
	@Before
	public void setUp() throws Exception {
        queueClient = new DownloadQueueClient("localhost", "downloadrequest");

	}

	@Test
	public void test() {
		String uuid = UUID.randomUUID().toString();
		DownloadRequest downloadRequest = new DownloadRequest(uuid);
		queueClient.sendDownloadRequest(downloadRequest);
		DownloadRequest receivedDownloadRequest = queueClient.receiveDownloadRequest();
		
		assertTrue("received downloadrequest expected to be equal to sent downloadrequest", 
				downloadRequest.equals(receivedDownloadRequest));
		
		try {
			queueClient.deleteDownloadRequest(receivedDownloadRequest);
		} catch (Exception e1) {
			fail("should not fail when deleting downloadRequest just received");
		}
		
		try {
			queueClient.deleteDownloadRequest(receivedDownloadRequest);
			fail("deleted same request from queue twice should throw exception!");
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
	}

}
