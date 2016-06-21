package data;

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
	
	/**
	 * Get metadata document.
	 * 
	 * @param id metadata document id
	 * @return retrieved metadata document or empty
	 */
	public Promise<Optional<MetadataDocument>> get(String id) {		
		return ws.url(config.getString("metadata.url") + id + ".xml")
			.setFollowRedirects(true)
			.get()
			.map(response -> {
				if(response.getStatus() == 200) {
					MetadataDocument metadataDocument = new MetadataDocument(response.asXml());
					
					String requiredUseLimitation = config.getString("metadata.required-use-limitation");
					if(requiredUseLimitation == null 
						|| metadataDocument.getUseLimitation()
							.contains(requiredUseLimitation)) {
						return Optional.of(metadataDocument);
					}
				} 
				
				return Optional.empty();
			});
	}
}