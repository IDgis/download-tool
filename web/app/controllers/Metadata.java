package controllers;

import java.io.ByteArrayOutputStream;

import javax.inject.Inject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Document;

import play.mvc.Result;
import play.mvc.Controller;
import play.libs.F.Promise;

import data.MetadataProvider;

public class Metadata extends Controller {
	
	private final MetadataProvider metadataProvider;
	
	@Inject
	public Metadata(MetadataProvider metadataProvider) {
		this.metadataProvider = metadataProvider;
	}
	
	public Promise<Result> get(String id) {
		return metadataProvider.get(id).map(metadataDocument -> {
			if(metadataDocument.isPresent()) {
				Document document = metadataDocument.get().getDocument();
				
				// remove existing stylesheet (if any)
				NodeList children = document.getChildNodes();
				for(int i = 0; i < children.getLength(); i++) {
					Node n = children.item(i);
					if(n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
						ProcessingInstruction pi = (ProcessingInstruction)n;
						if("xml-stylesheet".equals(pi.getTarget())) {
							document.removeChild(pi);
						}
					}
				}
				
				// add stylesheet
				document.insertBefore(
					document.createProcessingInstruction(
						"xml-stylesheet", 
						"type=\"text/xsl\" href=\"metadata.xsl\""),
					document.getFirstChild());
				
				// DOM -> byte[]
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer t = tf.newTransformer();
				t.transform(new DOMSource(document), new StreamResult(output));
				
				output.close();
				
				return ok(output.toByteArray()).as("application/xml");
			} else {
				return notFound();
			}
		});
	}
}