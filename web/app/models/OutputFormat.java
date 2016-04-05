package models;

/**
 * An download output format. Used to build the the download request form. 
 *
 */
public class OutputFormat {
	
	private final String name, title, mimeType, extension;
	
	public OutputFormat(String name, String title, String mimeType, String extension) {
		this.name = name;
		this.title = title;
		this.mimeType = mimeType;
		this.extension = extension;
	}
	
	public String name() {
		return name;
	}
	
	public String title() {
		return title;
	}

	public String mimeType() {
		return mimeType;
	}

	public String extension() {
		return extension;
	}
}