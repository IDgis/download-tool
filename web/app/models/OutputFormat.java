package models;

/**
 * An download output format. Used to build the the download request form. 
 *
 */
public class OutputFormat {
	
	private final String name, title;
	
	public OutputFormat(String name, String title) {
		this.name = name;
		this.title = title;
	}
	
	public String name() {
		return name;
	}
	
	public String title() {
		return title;
	}
}