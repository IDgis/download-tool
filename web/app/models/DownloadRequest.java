package models;

import play.data.validation.Constraints;

/**
 * A filled in download request.
 *
 */
public class DownloadRequest {	
	
	@Constraints.Required(message = "Je moet een keuze maken uit een van de formaten")
	private String format;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
}
