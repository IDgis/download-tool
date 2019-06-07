package models;

import java.util.List;

/**
 * Information about a download. Used to build the the download request form.
 *
 */
public class DownloadInfo {
	
	private final String title;
	
	private final String browseGraphicUrl;
	
	private final String description;
	
	private final List<OutputFormat> outputFormats;
	
	public DownloadInfo(String title, String browseGraphicUrl, String description, List<OutputFormat> outputFormats) {
		this.title = title;
		this.browseGraphicUrl = browseGraphicUrl;
		this.description = description;
		this.outputFormats = outputFormats;
	}
	
	public String title() {
		return title;
	}

	public String browseGraphicUrl() {
		return browseGraphicUrl;
	}

	public String description() {
		return description;
	}

	public List<OutputFormat> outputFormats() {
		return outputFormats;
	}
}
