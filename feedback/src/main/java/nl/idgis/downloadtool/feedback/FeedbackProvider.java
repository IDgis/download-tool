/**
 * 
 */
package nl.idgis.downloadtool.feedback;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.idgis.commons.utils.Mail;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;
import nl.idgis.downloadtool.domain.Feedback;
import nl.idgis.downloadtool.queue.FeedbackQueue;
import nl.idgis.downloadtool.queue.FeedbackQueueClient;

/**
 * The feedback provider polls a beanstalk queue for a Feedback object. 
 * It then saves the feedback to the database and assembles an email<br>
 * The queue it listens to and the email content is determined by configuration. 
 *
 * @author Rob
 */
public class FeedbackProvider {
	private static final Logger log = LoggerFactory.getLogger(FeedbackProvider.class);
	
	private final FeedbackQueue feedbackQueue;
	private final DownloadDao downloadDao;
	
	private String smtpHost, smtpUser, smtpPassword;
	private int smtpPort;
	private String subjectTemplate, msgTemplate, fromAddress;
	private String downloadUrl;

	public FeedbackProvider(FeedbackQueue feedbackQueue, DownloadDao downloadDao) {
		super();
		this.feedbackQueue = feedbackQueue;
		this.downloadDao = downloadDao;
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

	public String getSmptUser() {
		return smtpUser;
	}
	
	public void setSmptUser(String smptUser) {
		this.smtpUser = smptUser;
	}
	
	public String getSmptPassword() {
		return smtpPassword;
	}
	
	public void setSmptPassword(String smptPassword) {
		this.smtpPassword = smptPassword;
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

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	/**
	 * Poll a queue for feedback.<br>
	 * Save the feedback to the database and send an email.
	 * @throws Exception
	 */
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
			// assemble email
			Map<String,Object> placeholders = new HashMap<String,Object>();
			placeholders.put("username", downloadRequestInfo.getUserName());
			placeholders.put("featuretype", downloadRequestInfo.getDownload().getFt().getName());
			placeholders.put("downloadlink", downloadUrl + "/" + downloadRequestInfo.getRequestId());
			placeholders.put("responsecode", downloadResultInfo.getResponseCode());
			String subject = Mail.createMsg(placeholders, subjectTemplate);
			String msg = Mail.createMsg(placeholders, msgTemplate);
			
			/*
			 * send email
			 */
			try {
				if (smtpUser == null || smtpPassword == null || smtpUser.isEmpty() || smtpPassword.isEmpty()) {
					log.debug("Send email: [" + subject + "] to " + downloadRequestInfo.getUserEmailAddress());
					Mail.send(smtpHost, smtpPort, downloadRequestInfo.getUserEmailAddress(), fromAddress, subject, msg);
				} else {
					// send mail with authentication
					log.debug("Send authenticated email: [" + subject + "] to "
							+ downloadRequestInfo.getUserEmailAddress());
					Mail.send(smtpUser, smtpPassword, smtpHost, smtpPort, downloadRequestInfo.getUserEmailAddress(),
							smtpUser, subject, msg);
				}

			} catch (Exception e) {
				log.error("Exception while trying to send email: " + e.getMessage());
				e.printStackTrace();
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
	 * Set the configuration parameters and enter an eternal loop processing feedback.
	 * @param args not used
	 */
	public static void main(String[] args) {
		/*
		 * Get environment vars
		 */
		String host = System.getenv("BEANSTALK_HOST");
		if(host == null) {
			host = "localhost";
		}
		String feedbackTubeName = System.getenv("BEANSTALK_FEEDBACK_QUEUE");
		if(feedbackTubeName == null) {
			feedbackTubeName = "feedbackOkTube";
		}
		String smtpHost = System.getenv("SMTP_HOST");
		if(smtpHost == null) {
			smtpHost = "localhost";
		}
		String smtpPortStr = System.getenv("SMTP_PORT");
		if(smtpPortStr == null) {
			smtpPortStr = "25";
		}
		int smtpPort= Integer.parseInt(smtpPortStr);

		String smtpFromAddress = System.getenv("SMTP_FROMADDRESS");
		if(smtpFromAddress == null) {
			smtpFromAddress = "mail@idgis.nl";
		}
		
		// from docker-compose.override.yml		
		String smtpUser = System.getenv("SMTP_USER");
		String smtpPassword = System.getenv("SMTP_PASSWORD");
		
		String msgTemplate = System.getenv("EMAIL_MESSAGE_TEMPLATE");
		if(msgTemplate == null) {
			msgTemplate = "There is a downloadlink available for ${username} concerning ${featuretype}";
		}
		String subjectTemplate = System.getenv("EMAIL_SUBJECT_TEMPLATE");
		if(subjectTemplate == null) {
			subjectTemplate = "Geoportaal downloader: ${featuretype} is available for download";
		}
		String downloadUrl = System.getenv("DOWNLOAD_URL");
		if (downloadUrl==null){
			downloadUrl  = "localhost/downloadLink";
		}
		String dbUser = System.getenv("DB_USER");
		if (dbUser==null){
			dbUser = "postgres";
		}
		String dbPassword = System.getenv("DB_PW");
		if (dbPassword==null){
			dbPassword = "postgres";
		}
		String dbUrl = System.getenv("DB_URL");
		if (dbUrl==null){
			dbUrl = "jdbc:postgresql://localhost:5432/download";
		}
		
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl(dbUrl);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		
		try {
			log.info("start loop ");
			
			FeedbackQueueClient feedbackQueueClient = new FeedbackQueueClient(host, feedbackTubeName);
			DownloadDao downloadDao = new DownloadDao(dataSource);
			// setup provider
			FeedbackProvider fbp = new FeedbackProvider(feedbackQueueClient, downloadDao);
			fbp.setSmptUser(smtpUser);
			fbp.setSmptPassword(smtpPassword);
			fbp.setSmtpHost(smtpHost);
			fbp.setSmtpPort(smtpPort);
			fbp.setFromAddress(smtpFromAddress);
			fbp.setMsgTemplate(msgTemplate);
			fbp.setSubjectTemplate(subjectTemplate);
			fbp.setDownloadUrl(downloadUrl);			
			
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
