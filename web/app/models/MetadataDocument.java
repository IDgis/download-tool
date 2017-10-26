package models;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import play.Logger;
import play.Logger.ALogger;
import play.libs.XPath;

/**
 * A container for a metadata document. Encapsulates a DOM tree and 
 * provides the necessary getter to read specific xml elements.
 *
 */
public class MetadataDocument {
	
	private static final ALogger log = Logger.of(MetadataDocument.class);
	
	private final static Map<String, String> NS = namespaces();
	
	private final URL url;
	
	private final Document document;

	public MetadataDocument(URL url, Document document) {
		this.url = url;
		this.document = document;
	}
	
	private static Map<String, String> namespaces() {
		Map<String, String> ns = new HashMap<>();
		ns.put("gmd", "http://www.isotc211.org/2005/gmd");
		ns.put("gco", "http://www.isotc211.org/2005/gco");
		return Collections.unmodifiableMap(ns);
	}
	
	public URL getUrl() {
		return url;
	}
	
	public Document getDocument() {
		return document;
	}
	
	public Set<String> getresourceConstraints(String path) {
		NodeList nodeList = XPath.selectNodes(path, document, NS);
		
		HashSet<String> useLimitation = new HashSet<String>();
		for(int i = 0; i < nodeList.getLength(); i++) {
			useLimitation.add(nodeList.item(i).getTextContent());
		}
		
		return Collections.unmodifiableSet(useLimitation);
	}
	
	public String getWFSUrl() {
		return XPath.selectNode(
				"/gmd:MD_Metadata"
				+ "/gmd:distributionInfo"
				+ "/gmd:MD_Distribution"
				+ "/gmd:transferOptions"
				+ "/gmd:MD_DigitalTransferOptions"
				+ "/gmd:onLine"
				+ "/gmd:CI_OnlineResource[gmd:protocol/gco:CharacterString='OGC:WFS']"
				+ "/gmd:linkage"
				+ "/gmd:URL",
				document,
				NS).getTextContent();
	}
	
	public QName getFeatureTypeName() {
		Node node = XPath.selectNode(
				"/gmd:MD_Metadata"
				+ "/gmd:distributionInfo"
				+ "/gmd:MD_Distribution"
				+ "/gmd:transferOptions"
				+ "/gmd:MD_DigitalTransferOptions"
				+ "/gmd:onLine"
				+ "/gmd:CI_OnlineResource[gmd:protocol/gco:CharacterString='OGC:WFS']"
				+ "/gmd:name"
				+ "/gco:CharacterString",
				document,
				NS);
		
		String name = node.getTextContent();
		int separatorIdx = name.indexOf(":");
		if(separatorIdx == -1) {
			return new QName(XMLConstants.NULL_NS_URI, name);
		} else {
			String prefix = name.substring(0, separatorIdx);
			String localName = name.substring(separatorIdx + 1);
			
			String namespaceURI = node.lookupNamespaceURI(prefix);
			if(namespaceURI == null) {
				return new QName(XMLConstants.NULL_NS_URI, localName);
			} else {
				return new QName(namespaceURI, localName);
			}
		}
	}
	
	public String getTitle() {
		return XPath.selectNode(
			"/gmd:MD_Metadata"
			+ "/gmd:identificationInfo"
			+ "/gmd:MD_DataIdentification"
			+ "/gmd:citation"
			+ "/gmd:CI_Citation"
			+ "/gmd:title"
			+ "/gco:CharacterString",
			document,
			NS).getTextContent();
	}
	
	public List<String> getSupplementalInformationUrls() {
		NodeList nl = XPath.selectNodes(
			"/gmd:MD_Metadata"
			+ "/gmd:identificationInfo"
			+ "/gmd:MD_DataIdentification"
			+ "/gmd:supplementalInformation"
			+ "/gco:CharacterString",
			document,
			NS);
		
		ArrayList<String> result = new ArrayList<>();
		for(int i = 0; i < nl.getLength(); i++) {
			String text = nl.item(i).getTextContent();
			
			// TODO: to be removed when urls in metadata are repaired 
			text = text.replace('\\', '/');
			
			String[] textSplit = text.split("\\|");
			try {
				URL url = new URL(textSplit[textSplit.length - 1]);
				result.add(url.toExternalForm());
			} catch(MalformedURLException e) {
				log.warn("couldn't parse supplemental information url", e);
			}
		}
		
		return result;
	}
	
	public String getBrowseGraphicUrl() {
		String url = XPath.selectNode("/gmd:MD_Metadata"
			+ "/gmd:identificationInfo"
			+ "/gmd:MD_DataIdentification"
			+ "/gmd:graphicOverview"
			+ "/gmd:MD_BrowseGraphic"
			+ "/gmd:fileName"
			+ "/gco:CharacterString",
			document,
			NS).getTextContent();
		
		// TODO: to be removed when urls in metadata are repaired 
		url = url.replace('\\', '/');
		
		return url;
	}
	
	public Optional<URL> getStylesheet() throws MalformedURLException {
		NodeList children = document.getChildNodes();
		for(int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if(n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				ProcessingInstruction pi = (ProcessingInstruction)n;
				if("xml-stylesheet".equals(pi.getTarget())) {
					String data = pi.getData();
					
					Pattern p = Pattern.compile("(.*?)=.*?\"(.*?)\"");
					Matcher m = p.matcher(data);
					
					Map<String, String> kvp = new HashMap<>();
					while(m.find()) {
						String key = m.group(1).trim();
						String value = m.group(2).trim();
						
						kvp.put(key, value);
					}
					
					if(kvp.containsKey("href")) {
						return Optional.of(new URL(url, kvp.get("href")));
					}
				}
			}
		}
		
		return Optional.empty();
	}
}
