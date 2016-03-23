package nl.idgis.downloadtool.queue;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.beanstalkc.BeanstalkClientFactory;
import com.dinstone.beanstalkc.Configuration;
import com.dinstone.beanstalkc.Job;
import com.dinstone.beanstalkc.JobConsumer;
import com.dinstone.beanstalkc.JobProducer;
import com.google.gson.Gson;

import nl.idgis.downloadtool.domain.DownloadRequest;

/**
 * The queueClient implements a queue between Processor and DownloadRequestController.<br>
 * One queues is used for receiving downloadrequests<br>
 * <br>
 * This implementation uses beanstalkd as queuing mechanism .<br>
 * Objects are serialized over beanstalk as json.<br>
 *  
 * @see nl.idgis.downloadtool.downloader.DownloadProcessor
 *   
 * @author Rob
 *
 */
public class DownloadQueueClient implements DownloadQueue {
	private static final Logger log = LoggerFactory.getLogger(DownloadQueueClient.class);
	
	private final Gson gson ;
	
	/**
	 * Collection to keep job id's between receive from queue en delete from queue.<br>
	 * key is downloadrequest, value is job id.
	 */
	private final Map<DownloadRequest, Long> jobs;
	private JobProducer producer;
	private JobConsumer consumer;
	
	/**
	 * Construct a DownloadQueueCLient
	 * @param beanstalkHost
	 * @param beanstalkTubeName the identifier of the queue.
	 */
	public DownloadQueueClient(String beanstalkHost, String beanstalkTubeName) {
		super();

		Configuration config = new Configuration();
		config.setServiceHost(beanstalkHost);
		config.setServicePort(11300);
		
		BeanstalkClientFactory factory = new BeanstalkClientFactory(config);

		this.producer = factory.createJobProducer(beanstalkTubeName);
		this.consumer = factory.createJobConsumer(beanstalkTubeName);

		gson = new Gson();
		jobs = new HashMap<DownloadRequest, Long>();		
	}

	/**
	 * Construct a DownloadQueueCLient.<br>
	 * This constructor is useful for testing using mocks for producer and consumer
	 * @param producer beanstalk
	 * @param consumer beanstalk
	 */
	public DownloadQueueClient(JobProducer producer, JobConsumer consumer) {
		super();
		this.producer = producer;
		this.consumer = consumer;
		
		gson = new Gson();
		jobs = new HashMap<DownloadRequest, Long>();		
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#getDownloadRequest()
	 */
	@Override
	public DownloadRequest receiveDownloadRequest(){

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
		log.debug("downloadrequest received: '" + json + "', jobId: " + jobId);

		/*
		 * convert message to DownloadRequest object
		 */
		DownloadRequest downloadRequest = gson.fromJson(json, DownloadRequest.class);
		/*
		 * put job id in collection with downloadRequest as key
		 */
		jobs.put(downloadRequest, jobId);
		return downloadRequest;
	}
	
	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#deleteDownloadRequest(nl.idgis.downloadtool.domain.DownloadRequest)
	 */
	@Override
	public void deleteDownloadRequest(DownloadRequest downloadRequest) throws Exception{
		log.debug("delete downloadrequest " + downloadRequest);
		/*
		 * remove downloadRequest from collection 
		 */
		Long jobId = jobs.remove(downloadRequest);
		if (jobId == null){
			throw new Exception(downloadRequest + " does not exist!");
		}
		/*
		 * delete job from queue
		 */
		consumer.deleteJob(jobId);
	}
	
	
	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#sendDownloadRequest(nl.idgis.downloadtool.domain.DownloadRequest)
	 */
	@Override
	public void sendDownloadRequest(DownloadRequest downloadRequest) {
		log.debug("send downloadrequest " + downloadRequest);
		/*
		 * convert downloadrequest to json string
		 */
		
		String json = gson.toJson(downloadRequest);
		/* 
		 * send json to queue
		 */
		try {
			Long jobId = producer.putJob(0, 0, 1, json.getBytes("utf-8"));
			log.debug("sent downloadrequest '" + json + "', jobId: " + jobId);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

}
