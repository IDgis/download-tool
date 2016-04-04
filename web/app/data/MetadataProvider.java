package data;

import java.util.Optional;

import javax.inject.Inject;

import models.MetadataDocument;

import play.Configuration;
import play.libs.ws.WSClient;
import play.libs.F.Promise;

public class MetadataProvider {
	
	private final Configuration config; 
	
	private final WSClient ws;

	@Inject	
	public MetadataProvider(Configuration config, WSClient ws) {
		this.config = config;
		this.ws = ws;
	}
	
	public Promise<Optional<MetadataDocument>> get(String id) {		
		return ws.url(config.getString("metadata.url") + id + ".xml")
			.setFollowRedirects(true)
			.get()
			.map(response -> {
				if(response.getStatus() == 200) {
					return Optional.of(new MetadataDocument(response.asXml()));
				} else {
					return Optional.empty();
				}
			});
	}
}