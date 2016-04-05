package nl.idgis.downloadtool.domain;

import java.sql.Timestamp;

/**
 * Represents the download_request_job table in the downloadBean database.<br>
 * 
 * @author Rob
 *
 */
public class DownloadRequestInfo {

	private long id; // unique pk
    
	private String requestId; // unique logische sleutel (uuid)
    
	private Timestamp requestTime; // not null
    
	private Download download; //json  of jsonb ?
    
    /* 
     * items from form
     */
	private String jobId; // identifies the queue job_id 
    
	private String userName;
    
	private String userEmailAddress;
    
	private String userFormat; // mime type the user wants to receive the data in (shp, kml, gml or dxf)

	public DownloadRequestInfo() {
		super();
	}
    
	public DownloadRequestInfo(String requestId, String jobId, String userName, String userEmailAddress, String userFormat, Download download) {
		super();
		this.requestId = requestId; 
		this.jobId = jobId; 
		this.userName = userName; 
		this.userEmailAddress = userEmailAddress; 
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

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserEmailAddress() {
		return userEmailAddress;
	}

	public void setUserEmailAddress(String userEmailAddress) {
		this.userEmailAddress = userEmailAddress;
	}

	public String getUserFormat() {
		return userFormat;
	}

	public void setUserFormat(String userFormat) {
		this.userFormat = userFormat;
	}

    
}
