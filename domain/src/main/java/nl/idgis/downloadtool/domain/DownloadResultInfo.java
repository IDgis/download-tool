package nl.idgis.downloadtool.domain;

import java.sql.Timestamp;

/**
 * Represents the download_result_job table in the downloadBean database.<br>
 * 
 * @author Rob
 *
 */
public class DownloadResultInfo {

	private String requestId; // unique logische sleutel

	private Timestamp responseTime; // not null

	private String responseCode; // not null

	public DownloadResultInfo() {
		super();
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Timestamp getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(Timestamp responseTime) {
		this.responseTime = responseTime;
	}

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

}
