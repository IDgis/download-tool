package models;

import java.util.List;

public class DownloadInfo {
	
	private final String title;
	
	private final List<OutputFormat> outputFormats;
	
	public DownloadInfo(String title, List<OutputFormat> outputFormats) {
		this.title = title;
		this.outputFormats = outputFormats;
	}
	
	public String title() {
		return title;
	}

	public List<OutputFormat> outputFormats() {
		return outputFormats;
	}
}
