package nl.idgis.downloadtool.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Description of non-conversion data<br>
 * Will be retrieved by a http GET request.
 * 
 * @author Rob
 *
 */
public class AdditionalData implements Serializable {
	private static final long serialVersionUID = -2614992520934958392L;

	String name; // filename of the additional data
	String url; // url for get request to retrieve the additional data
	Map<String, String> headers; // http headers to send with the http request

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = new HashMap<>(headers);
	}
	
	public Map<String, String> getHeaders() {
		if(headers == null) {
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(headers);
		}
	}

}
