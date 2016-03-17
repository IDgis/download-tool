package nl.idgis.downloadtool.queue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	/**
	 * Collection to keep jobs between receive from queue en delete from queue.<br>
	 * key is downloadrequest, value is job id.
	 */
	private final Map<DownloadRequest, String> jobs;
	private String beanstalkTubeName;
	//TODO remove when beanstalk is implemented
	String jobId;
	
	/**
	 * Construct a DownloadQueueCLient
	 * @param beanstalkTubeName the identifier of the queue.
	 */
	public DownloadQueueClient(String beanstalkTubeName) {
		super();
		this.beanstalkTubeName = beanstalkTubeName;
		jobs = new HashMap<DownloadRequest,String>();
	}

	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#getDownloadRequest()
	 */
	@Override
	public DownloadRequest receiveDownloadRequest(){
		// TODO replace with beanstalk code
		for (DownloadRequest downloadRequest : jobs.keySet()) {		
			if (this.jobId.equals(jobs.get(downloadRequest))){
				log.debug("receive downloadrequest " + downloadRequest);
				return downloadRequest;
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see nl.idgis.downloadtool.queue.DownloadQueue#deleteDownloadRequest(nl.idgis.downloadtool.domain.DownloadRequest)
	 */
	@Override
	public void deleteDownloadRequest(DownloadRequest downloadRequest) throws Exception{
		log.debug("delete downloadrequest " + downloadRequest);
		// TODO beanstalk code
		String jobId = jobs.remove(downloadRequest);
		if (jobId == null){
			throw new Exception(downloadRequest + " does not exist!");
		}
	}
	
	
	@Override
	public void sendDownloadRequest(DownloadRequest downloadRequest) {
		log.debug("send downloadrequest " + downloadRequest);
		// TODO beanstalk code
		this.jobId = UUID.randomUUID().toString();
		jobs.put(downloadRequest, this.jobId);
		
	}

}
