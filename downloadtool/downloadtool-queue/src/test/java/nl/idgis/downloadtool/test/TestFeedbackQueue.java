package nl.idgis.downloadtool.test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.beanstalkc.Job;
import com.dinstone.beanstalkc.JobConsumer;
import com.dinstone.beanstalkc.JobProducer;
import com.google.gson.Gson;

import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

public class TestFeedbackQueue  extends EasyMockSupport {
	private static final Logger log = LoggerFactory.getLogger(TestFeedbackQueue.class);
	
	private JobProducer producerMock;
	private JobConsumer consumerMock;
	
	private Feedback feedback; 
	private final Gson gson = new Gson();
	private String json;
	private byte[] data;
	
	@Before
	public void setUp() throws Exception {
		String uuid = UUID.randomUUID().toString();
		feedback = new Feedback(uuid);
		feedback.setResultCode("OK");
		json = gson.toJson(feedback);
		log.debug("test json: " +json);
		data = json.getBytes("utf-8");
	}

	@Test
	public void testSendReceiveDelete() {
		Long jobId = 5678L;
		
		Job job = mock(Job.class);
		expect(job.getId()).andReturn(jobId);
		expect(job.getData()).andReturn(data);
		replay(job);
		
		consumerMock = mock(JobConsumer.class);
		expect(consumerMock.reserveJob(0)).andReturn(job);
		expect(consumerMock.deleteJob(jobId)).andReturn(true);
		replay(consumerMock);
		
		producerMock = mock(JobProducer.class);
		expect(producerMock.putJob(eq(0), eq(0), eq(1), anyObject(byte[].class))).andReturn(jobId);
		replay(producerMock);
		
		FeedbackQueue queueClient = new FeedbackQueueClient(producerMock, consumerMock);	
		queueClient.sendFeedback(feedback);
		Feedback receivedFeedback = queueClient.receiveFeedback();
		
		assertTrue("received Feedback expected to be equal to sent Feedback", 
				feedback.getRequestId().equals(receivedFeedback.getRequestId()));
		
		try {
			queueClient.deleteFeedback(receivedFeedback);
		} catch (Exception e1) {
			fail("should not fail when deleting Feedback just received");
		}
		
		try {
			queueClient.deleteFeedback(receivedFeedback);
			fail("deleted same request from queue twice should throw exception!");
		} catch (Exception e) {
			log.debug("Expected an exception: " + e.getMessage());
		}
		
		verifyAll();
	}

}
