package nl.idgis.downloadtool.queue;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.beanstalkc.BeanstalkClientFactory;
import com.dinstone.beanstalkc.Configuration;
import com.dinstone.beanstalkc.Job;
import com.dinstone.beanstalkc.JobConsumer;
import com.dinstone.beanstalkc.JobProducer;
import com.google.gson.Gson;

import nl.idgis.downloadtool.domain.Feedback;

/**
 * The feedbackClient implements a queue between Processor and FeedbackProvider.<br>
 * <br>
 * This implementation uses beanstalkd as queuing mechanism .<br>
 * Objects are serialized over beanstalk as json.<br>
 *  
 * @see nl.idgis.downloadtool.downloader.DownloadProcessor
 *   
 * @author Rob
 *
 */
public class FeedbackQueueClient implements FeedbackQueue {
	private static final Logger log = LoggerFactory.getLogger(FeedbackQueueClient.class);
	
	private final Gson gson ;
	
	/**
	 * Collection to keep jobs between get from queue en delete from queue.<br>
	 * key is feedback, value is job id.
	 */
	private final Map<Feedback, Long> jobs;
	private final JobProducer producer;
	private final JobConsumer consumer;
	
	/**
	 * Construct a FeedbackQueueClient
	 * @param beanstalkHost
	 * @param beanstalkTubeName the identifier of the queue.
	 */
	public FeedbackQueueClient(String beanstalkHost, String beanstalkTubeName) {
		super();

		Configuration config = new Configuration();
		config.setServiceHost(beanstalkHost);
		config.setServicePort(11300);
		
		BeanstalkClientFactory factory = new BeanstalkClientFactory(config);

		this.producer = factory.createJobProducer(beanstalkTubeName);
		this.consumer = factory.createJobConsumer(beanstalkTubeName);

		this.gson = new Gson();
		this.jobs = new HashMap<Feedback, Long>();		
	}

	/**
	 * Construct a FeedbackQueueClient.<br>
	 * This constructor is useful for testing using mocks for producer and consumer
	 * @param producer beanstalk
	 * @param consumer beanstalk
	 */
	public FeedbackQueueClient(JobProducer producer, JobConsumer consumer) {
		super();
		this.producer = producer;
		this.consumer = consumer;
		
		this.gson = new Gson();
		this.jobs = new HashMap<Feedback, Long>();		
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.FeedbackQueue#sendFeedback(nl.idgis.downloadtool.domain.Feedback)
	 */
	@Override
	public void sendFeedback(Feedback feedback){
		log.debug("send feedback " + feedback);
		/*
		 * convert feedback to json string
		 */
		
		String json = gson.toJson(feedback);
		/* 
		 * send json to queue
		 */
		try {
			Long jobId = producer.putJob(0, 0, 1, json.getBytes("utf-8"));
			log.debug("sent json '" + json + "', jobId: " + jobId);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.FeedbackQueue#receiveFeedback()
	 */
	@Override
	public Feedback receiveFeedback() {
		Job job = consumer.reserveJob(0);
		Long jobId = job.getId();
		
		/*
		 * retrieve json message from queue
		 */
		String json = null;
		try {
			json = new String(job.getData(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		log.debug("json received: '" + json + "', jobId: " + jobId);

		/*
		 * convert message to feedback object
		 */
		Feedback feedback = gson.fromJson(json, Feedback.class);
		/*
		 * put job id in collection with feedback as key
		 */
		jobs.put(feedback, jobId);
		return feedback;
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.FeedbackQueue#deleteFeedback(nl.idgis.downloadtool.domain.Feedback)
	 */
	@Override
	public void deleteFeedback(Feedback feedback) throws Exception {
		log.debug("delete feedback " + feedback);
		/*
		 * remove feedback from collection 
		 */
		Long jobId = jobs.remove(feedback);
		if (jobId == null){
			throw new Exception(feedback + " does not exist!");
		}
		/*
		 * delete job from queue
		 */
		consumer.deleteJob(jobId);
		
	}
}
