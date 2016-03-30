/**
 * 
 */
package nl.idgis.downloadtool.feedback;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.commons.utils.Mail;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * @author Rob
 *
 */
public class FeedbackProvider {
	private static final Logger log = LoggerFactory.getLogger(FeedbackProvider.class);

	private static final String BEANSTALK_HOST = "BEANSTALK_HOST";
	private static final String BEANSTALK_FEEDBACK_QUEUE = "BEANSTALK_FEEDBACK_QUEUE";

	private static final String DB_URL = "DB_URL";
	private static final String DB_USER = "DB_USER";
	private static final String DB_PW = "DB_PW";
	
	private static final String SMTP_HOST = "SMTP_HOST";

	private static final String SMTP_PORT = "SMTP_PORT";

	private static final String SMTP_FROMADDRESS = "SMTP_FROMADDRESS";

	private static final String EMAIL_MESSAGE_TEMPLATE = "EMAIL_MESSAGE_TEMPLATE";

	private static final String EMAIL_SUBJECT_TEMPLATE = "EMAIL_SUBJECT_TEMPLATE";
	
	private FeedbackQueue feedbackQueue;
	private DownloadDao downloadDao;
	
	private String smtpHost;
	private int smtpPort;
	private String subjectTemplate, msgTemplate, fromAddress;

	public FeedbackProvider(FeedbackQueue feedbackQueue, DownloadDao downloadDao) {
		super();
		this.feedbackQueue = feedbackQueue;
		this.downloadDao = downloadDao;
	}

    public void setFeedbackQueue(FeedbackQueue queueClient){
    	this.feedbackQueue = queueClient;
    }
    
	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public int getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getSubjectTemplate() {
		return subjectTemplate;
	}

	public void setSubjectTemplate(String subjectTemplate) {
		this.subjectTemplate = subjectTemplate;
	}

	public String getMsgTemplate() {
		return msgTemplate;
	}

	public void setMsgTemplate(String msgTemplate) {
		this.msgTemplate = msgTemplate;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public void processFeedback() throws Exception {
		/*
		 * get feedback from queue
		 */
		Feedback feedback = feedbackQueue.receiveFeedback();
		log.debug("Feedback received: " + feedback);

		/*
		 * put feedback in db
		 */
		DownloadResultInfo downloadResultInfo = new DownloadResultInfo(feedback);
		downloadDao.createDownloadResultInfo(downloadResultInfo);
		
		/* 
		 * assemble email from:
		 *   - database data using feedback.getRequestId()
		 *   - email templates from configuration 
		 */
		DownloadRequestInfo downloadRequestInfo = downloadDao.readDownloadRequestInfo(feedback.getRequestId());
		if (downloadRequestInfo != null){
			// TODO assemble email
			Map<String,Object> placeholders = new HashMap<String,Object>();
			placeholders.put("username", downloadRequestInfo.getUserName());
			placeholders.put("featuretype", downloadRequestInfo.getDownload().getFt().getName());
			String subject = Mail.createMsg(placeholders, subjectTemplate);
			String msg = Mail.createMsg(placeholders, msgTemplate);
			
			/*
			 * send email
			 */
			log.debug("Send email: [" + subject + "] to " + downloadRequestInfo.getUserEmailAddress());
			 try {
				Mail.send(smtpHost, smtpPort, downloadRequestInfo.getUserEmailAddress(), fromAddress, subject, msg);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.debug("Exception while trying to send email: " + e.getMessage());
			}
		} else {
			// not to be expected but no action
			log.debug("not found requestinfo in db for id: " + feedback.getRequestId());
		}
		/*
		 * delete feedback item from queue
		 */
		feedbackQueue.deleteFeedback(feedback);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Get environment vars
		 */
		String host = System.getenv(BEANSTALK_HOST);
		if(host == null) {
			host = "localhost";
		}
		String feedbackTubeName = System.getenv(BEANSTALK_FEEDBACK_QUEUE);
		if(feedbackTubeName == null) {
			feedbackTubeName = "feedbackOkTube";
		}
		String smtpHost = System.getenv(SMTP_HOST);
		if(smtpHost == null) {
			smtpHost = "localhost";
		}
		String smtpPortStr = System.getenv(SMTP_PORT);
		if(smtpPortStr == null) {
			smtpPortStr = "25";
		}
		int smtpPort= Integer.parseInt(smtpPortStr);

		String smtpFromAddress = System.getenv(SMTP_FROMADDRESS);
		if(smtpFromAddress == null) {
			smtpFromAddress = "mail@idgis.nl";
		}
		String msgTemplate = System.getenv(EMAIL_MESSAGE_TEMPLATE);
		if(msgTemplate == null) {
			msgTemplate = "There is a downloadlink available for ${username} concerning ${featuretype}";
		}
		String subjectTemplate = System.getenv(EMAIL_SUBJECT_TEMPLATE);
		if(subjectTemplate == null) {
			subjectTemplate = "Geoportaal downloader: ${featuretype} is available for download";
		}
		String dbUser = System.getenv(DB_USER);
		if (dbUser==null){
			dbUser = "postgres";
		}
		String dbPassword = System.getenv(DB_PW);
		if (dbPassword==null){
			dbPassword = "postgres";
		}
		String dbUrl = System.getenv(DB_URL);
		if (dbUrl==null){
			dbUrl = "jdbc:postgresql://localhost:5432/download";
		}
		
		try {
			log.info("start loop ");
			
			FeedbackQueueClient feedbackQueueClient = new FeedbackQueueClient(host, feedbackTubeName);
			DownloadDao downloadDao = new DownloadDao(dbUrl, dbUser, dbPassword);
			// setup provider
			FeedbackProvider fbp = new FeedbackProvider(feedbackQueueClient, downloadDao);
			fbp.setSmtpHost(smtpHost);
			fbp.setSmtpPort(smtpPort);
			fbp.setFromAddress(smtpFromAddress);
			fbp.setMsgTemplate(msgTemplate);
			fbp.setSubjectTemplate(subjectTemplate);
			
			for (;;) {
				log.debug("processFeedback");
				fbp.processFeedback();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("end loop: " + e.getMessage());
		}

	}

}
