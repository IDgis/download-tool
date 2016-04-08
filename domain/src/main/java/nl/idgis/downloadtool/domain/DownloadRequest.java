package nl.idgis.downloadtool.domain;

import java.io.Serializable;

/**
 * Bean containing all information to perform a WFS download with additional
 * data.<br>
 * Contains the request id, a download bean and the mimetype format the user
 * wants to receive for wfs data.<br>
 * 
 * 
 * @author Rob
 *
 */
public class DownloadRequest implements Serializable {

	private static final long serialVersionUID = 3501190635214364900L;

	/**
	 * requestId bevat requestId van dit download verzoek, wordt gebruikt bij
	 * terugmeldingen
	 */
	private String requestId;

	/**
	 * Mimetype to convert to e.g. "application/vnd.google-earth.kml+xml"
	 */
	private String convertToMimetype;

	private Download download;

	/**
	 * @param id
	 *            unique identifier of this downloadrequest.<br>
	 *            Equal to Feedback.id
	 */
	public DownloadRequest(String id) {
		super();
		this.requestId = id;
	}

	public String getRequestId() {
		return requestId;
	}

	public String getConvertToMimetype() {
		return convertToMimetype;
	}

	public void setConvertToMimetype(String convertToMimetype) {
		this.convertToMimetype = convertToMimetype;
	}

	public Download getDownload() {
		return download;
	}

	public void setDownload(Download download) {
		this.download = download;
	}

	@Override
	public String toString() {
		return "DownloadRequest [requestId=" + requestId + ", convertToMimetype=" + convertToMimetype + "]";
	}

}
