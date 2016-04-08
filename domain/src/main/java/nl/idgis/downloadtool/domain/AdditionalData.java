package nl.idgis.downloadtool.domain;

import java.io.Serializable;

/**
 * Description of non-conversion data<br>
 * Will be retrieved by a http GET request.
 * 
 * @author Rob
 *
 */
public class AdditionalData implements Serializable {
	private static final long serialVersionUID = -4617992230934918397L;

	String name; // filename of the additional data
	String extension; // file extension of the additional data
	String Url; // url for get request to retrieve the additional data

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public String getUrl() {
		return Url;
	}

	public void setUrl(String url) {
		Url = url;
	}

}
