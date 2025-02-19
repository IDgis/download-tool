package models;

/**
 * Status about the download. If it is ready or not and if so if it has been successful.
 *
 */
public class DownloadStatus {
	
	private final Boolean ready;
	
	private final Boolean success;
	
	private final String url;
	
	public DownloadStatus(Boolean ready, Boolean success, String url) {
		this.ready = ready;
		this.success = success;
		this.url = url;
	}
	
	public Boolean ready() {
		return ready;
	}

	public Boolean success() {
		return success;
	}

	public String url() {
		return url;
	}
}
