package data;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import models.MetadataDocument;

import com.typesafe.config.Config;

import play.libs.ws.WSClient;

/**
 * A component responsible for retrieving metadata documents.
 *
 */
public class MetadataProvider {

	private final Config config;

	private final WSClient ws;

	@Inject
	public MetadataProvider(Config config, WSClient ws) {
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
	public CompletionStage<Optional<MetadataDocument>> get(String id) {
		try {
			URL url = new URL(config.getString("metadata.url") + id + ".xml");
			return ws.url(url.toExternalForm())
				.setFollowRedirects(true)
				.setHeader(getTrustedHeader(), getTrustedValue())
				.get()
				.thenApply(response -> {
					if(response.getStatus() == 200) {
						MetadataDocument metadataDocument = new MetadataDocument(url, response.asXml());

						String confidentialPath = config.getString("metadata.confidential-path");
						String dataPublicValue = config.hasPath("metadata.data-public-value")
							? config.getString("metadata.data-public-value")
							: null;
						if(dataPublicValue == null
							|| metadataDocument.getresourceConstraints(confidentialPath)
								.contains(dataPublicValue)
							|| "intern".equals(config.getString("download.access"))) {
							return Optional.of(metadataDocument);
						}
					}

					return Optional.<MetadataDocument>empty();
				});
		} catch(MalformedURLException e) {
			CompletableFuture<Optional<MetadataDocument>> failed = new CompletableFuture<>();
			failed.completeExceptionally(e);
			return failed;
		}
	}
}