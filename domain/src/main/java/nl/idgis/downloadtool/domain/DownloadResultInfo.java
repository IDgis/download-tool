package nl.idgis.downloadtool.domain;

public class DownloadResultInfo {

	private String requestId;
	private String responseCode;

	public DownloadResultInfo(String requestId, String responseCode) {
		this.requestId = requestId;
		this.responseCode = responseCode;
	}

	public String getRequestId() {
		return requestId;
	}

	public String getResponseCode() {
		return responseCode;
	}
}
