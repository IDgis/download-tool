package models;

import play.data.validation.Constraints;

/**
 * A filled in download request.
 *
 */
public class DownloadRequest {	
	
	@Constraints.Required(message = "Je moet een keuze maken uit een van de formaten")
	private String format;
	
	@Constraints.Required(message = "Vul je eigen naam in")
	private String name;
	
	@Constraints.Email(message = "Het ingevulde e-mailadres is niet correct")
	@Constraints.Required(message = "Vul hier je e-mailadres in, waarheen de downloadlink moet worden gestuurd")
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
