package nl.idgis.downloadtool.domain;

import java.sql.Timestamp;
import java.util.List;

/**
 * Represents the download_request_job table in the downloadBean database.<br>
 * 
 * @author Rob
 *
 */
public class DownloadRequestInfo {

    long id; // unique pk
    
    String requestId; // unique logische sleutel (uuid)
    
    Timestamp requestTime; // not null
    
    Download download; //json  of jsonb ?
    
    /* 
     * items from form
     */
    String uuid; // identifies the dataset 
    
    String userName;
    
    String userEmailAddress;
    
    String userFormat; // mime type the user wants to receive the data in (shp, kml, gml or dxf)

	public DownloadRequestInfo(String requestId, String uuid, String userName, String userEmailAddress, String userFormat, Download download) {
		super();
		this.requestId = requestId; 
		this.uuid = uuid; 
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
