package data;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Optional;

import javax.inject.Inject;

import models.MetadataDocument;

import play.Configuration;
import play.libs.ws.WSClient;
import play.libs.F.Promise;

/**
 * A component responsible for retrieving metadata documents.
 *
 */
public class MetadataProvider {
	
	private final Configuration config; 
	
	private final WSClient ws;

	@Inject	
	public MetadataProvider(Configuration config, WSClient ws) {
		this.config = config;
		this.ws = ws;
	}
	
	public String getTrustedHeader() {
		return config.getString("download.trusted.header");
	}
	
	public String getTrustedValue() {
		String access = config.getString("download.access");
		if("intern".equals(access)) {
			return "1";
		} else {
			return "0";
		}
	}
	
	/**
	 * Get metadata document.
	 * 
	 * @param id metadata document id
	 * @return retrieved metadata document or empty
	 */
	public Promise<Optional<MetadataDocument>> get(String id) {
		try {
			URL url = new URL(config.getString("metadata.url") + id + ".xml");
			return ws.url(url.toExternalForm())
				.setFollowRedirects(true)
				.setHeader(getTrustedHeader(), getTrustedValue())
				.get()
				.map(response -> {
					if(response.getStatus() == 200) {
						MetadataDocument metadataDocument = new MetadataDocument(url, response.asXml());
						
						String confidentialPath = config.getString("metadata.confidential-path");
						String dataPublicValue = config.getString("metadata.data-public-value");
						if(dataPublicValue == null 
							|| metadataDocument.getresourceConstraints(confidentialPath)
								.contains(dataPublicValue)
							|| "intern".equals(config.getString("download.access"))) {
							return Optional.of(metadataDocument);
						}
					} 
					
					return Optional.empty();
				});
		} catch(MalformedURLException e) {
			return Promise.throwing(e);
		}
	}
}