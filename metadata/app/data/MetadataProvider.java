package data;

import java.util.Optional;

import javax.inject.Inject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

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
					Document d = response.asXml();
					
					// remove existing stylesheet (if any)
					NodeList children = d.getChildNodes();
					for(int i = 0; i < children.getLength(); i++) {
						Node n = children.item(i);
						if(n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
							ProcessingInstruction pi = (ProcessingInstruction)n;
							if("xml-stylesheet".equals(pi.getTarget())) {
								d.removeChild(pi);
							}
						}
					}
					
					// add stylesheet
					d.insertBefore(
							d.createProcessingInstruction(
								"xml-stylesheet", 
								"type=\"text/xsl\" href=\"metadata.xsl\""),
							d.getFirstChild());
					
					return Optional.of(new MetadataDocument(d));
				} else {
					return Optional.empty();
				}
			});
	}
}