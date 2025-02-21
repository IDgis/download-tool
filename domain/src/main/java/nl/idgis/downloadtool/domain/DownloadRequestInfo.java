package nl.idgis.downloadtool.domain;

import java.sql.Timestamp;

/**
 * Represents the download_request_job table in the downloadBean database.<br>
 * 
 * @author Rob
 *
 */
public class DownloadRequestInfo {

	private String requestId; // unique logische sleutel (uuid)

	private Timestamp requestTime; // not null

	private Download download; // json of jsonb ?

	/*
	 * items from form
	 */
	private String jobId; // identifies the queue job_id

	private String userFormat; // mime type the user wants to receive the data

	public DownloadRequestInfo(
			String requestId, 
			String jobId,
			String userFormat, 
			Download download) {
		super();
		this.requestId = requestId;
		this.jobId = jobId;
		this.userFormat = userFormat;
		this.download = download;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Timestamp getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(Timestamp requestTime) {
		this.requestTime = requestTime;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public Download getDownload() {
		return download;
	}

	public void setDownload(Download download) {
		this.download = download;
	}

	public String getUserFormat() {
		return userFormat;
	}

	public void setUserFormat(String userFormat) {
		this.userFormat = userFormat;
	}

}
