package models;

import play.data.validation.Constraints;

public class DownloadRequest {	
	
	@Constraints.Required
	private String format;
	
	@Constraints.Required
	private String name;
	
	@Constraints.Email
	@Constraints.Required
	private String email;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
