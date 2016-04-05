/**
 * 
 */
package nl.idgis.downloadtool.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import nl.idgis.downloadtool.domain.WfsFeatureType;

/**
 * This class represents a WFS as the source of the download.<br>
 * It is constructed with a url that represents the source and then provides an
 * inputstream from that url.<br>
 * 
 * @author Rob
 * 
 */
public class DownloadWfs implements DownloadSource {
	public static String newLine = System.getProperty("line.separator");
	private static final Log log = LogFactory.getLog(DownloadWfs.class);
	
	private int maxFeatures = 0;
	
	private URI uri;
	private HttpEntity entity;
	private CloseableHttpResponse response; 
	private CloseableHttpClient httpclient;
	private HttpPost httpPost;
	private HttpGet httpGet;
	

	/**
	 * Constructor that builds a WFS request.
	 * @param wfsFeatureType containing all information for the building the WFS request
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	public DownloadWfs(WfsFeatureType wfsFeatureType) throws MalformedURLException,
	UnsupportedEncodingException, URISyntaxException {
		this(wfsFeatureType, 0);
	}
	
	/**
	 * Constructor that builds a WFS request.
	 * @param wfsFeatureType containing all information for the building the WFS request
	 * @param maxFeatures nr of features to download, 0 is unrestricted
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	public DownloadWfs(WfsFeatureType wfsFeatureType, int maxFeatures) throws MalformedURLException,
			UnsupportedEncodingException, URISyntaxException {
		this.maxFeatures = maxFeatures;
		String serviceUrl = correctServiceUrl(wfsFeatureType.getServiceUrl());
		uri = new URL(serviceUrl).toURI();

		// For now no authentication is foreseen
		String userAuth = null;
		String pwAuth = null;
		httpclient = getHttpClient(serviceUrl, userAuth, pwAuth);
		
		makeGet(wfsFeatureType);
	}

	@Override
	public InputStream open() throws IOException {
		response = httpclient.execute(httpGet);
		log.debug("Http response: " + response.getStatusLine());
		entity = response.getEntity();
		// do something useful with the response body
		return entity.getContent();
	}

	@Override
	public void close() throws IOException {
		try {
			// After doing something useful with the response body,
			// ensure it is fully consumed:
			if (entity != null)
				EntityUtils.consume(entity);
		} finally {
			if (response != null)
				response.close();
			if (httpclient != null)
				httpclient.close();
		}
	}
 
	@Override
	public URI getUri() {
		return uri;
	}
	
	public int getMaxFeatures() {
		return maxFeatures;
	}

	public void setMaxFeatures(int maxFeatures) {
		this.maxFeatures = maxFeatures;
	}

	private CloseableHttpClient getHttpClient(String serviceUrl, String userAuth, String pwAuth) {
		CloseableHttpClient httpClient;
		if (userAuth == null || pwAuth == null || userAuth.isEmpty() || pwAuth.isEmpty()) {
			// no authentication
			httpClient = HttpClients.createDefault();
		} else {
			// Basic Authentication setup
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(userAuth, pwAuth));
			httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		}
		return httpClient;
	}

	private void makePost(WfsFeatureType wfsFeatureType)
			throws UnsupportedEncodingException {
		String xmlStr = null;
		HttpEntity entity = null;
		httpPost = new HttpPost(uri);
		
		String defaultCrs = wfsFeatureType.getCrs()==null?"urn:x-ogc:def:crs:EPSG:28992":wfsFeatureType.getCrs();
		String defaultVersion = wfsFeatureType.getServiceVersion()==null?"2.0.0":wfsFeatureType.getServiceVersion();

		xmlStr = makePostXml(defaultCrs, wfsFeatureType.getFilterExpression(), wfsFeatureType.getNamespacePrefix(), 
				wfsFeatureType.getNamespaceUri(), wfsFeatureType.getName(), defaultVersion, wfsFeatureType.getWfsMimetype());
		
		if (log.isTraceEnabled())
			log.trace("GetFeature xml: " + newLine + xmlStr);
		entity = new StringEntity(xmlStr, ContentType.APPLICATION_XML);
		httpPost.setEntity(entity);
	}

	private void makeGet(WfsFeatureType wfsFeatureType)
			throws UnsupportedEncodingException, MalformedURLException, URISyntaxException{
		String xmlStr = null;
		
		String defaultCrs = wfsFeatureType.getCrs()==null?"urn:x-ogc:def:crs:EPSG:28992":wfsFeatureType.getCrs();
		String defaultVersion = wfsFeatureType.getServiceVersion()==null?"2.0.0":wfsFeatureType.getServiceVersion();

		xmlStr = makeGet(defaultCrs, wfsFeatureType.getFilterExpression(), wfsFeatureType.getNamespacePrefix(), 
				wfsFeatureType.getNamespaceUri(), wfsFeatureType.getName(), defaultVersion, wfsFeatureType.getWfsMimetype());
		
		if (log.isTraceEnabled())
			log.trace("GetFeature url: " + newLine + xmlStr);
		uri = new URL(xmlStr).toURI();
		httpGet = new HttpGet(uri);
	}

	private String makePostXml(String crs, String filterExpression, 
			String typePrefix, String typeNameSpace, String typeName, String version, 
			String wfsFormaat) throws UnsupportedEncodingException {
		
		String unescapedFilterExpression = StringEscapeUtils.unescapeXml(filterExpression);
		
		StringBuilder sb = new StringBuilder();
		if (version.equals("2.0.0")){
			sb.append("<wfs:GetFeature xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
			  + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd\""
			  + " xmlns:gml=\"http://www.opengis.net/gml\""
			  + " xmlns:wfs=\"http://www.opengis.net/wfs/2.0\""
			  + " xmlns:fes=\"http://www.opengis.net/fes/2.0\""
			  + " xmlns:ogc=\"http://www.opengis.net/ogc\" \n");
		}else{
			sb.append("<wfs:GetFeature xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
					  + " xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\""
					  + " xmlns:gml=\"http://www.opengis.net/gml\""
					  + " xmlns:wfs=\"http://www.opengis.net/wfs\""
					  + " xmlns:ogc=\"http://www.opengis.net/ogc\" \n");
		}
		// attributes
		sb.append(" service=\"WFS\"\n"); 
		sb.append(" version=\"" + version + "\"\n");
		if (maxFeatures > 0){
			if (version.indexOf("2.0") > -1){
				// WFS version 2.0.x
				sb.append(" count=\"" + maxFeatures + "\"\n"); 			
			} else {
				// WFS version 1.1.0
				sb.append(" maxFeatures=\"" + maxFeatures + "\"\n"); 
			}
		}
		sb.append(" outputFormat=\"" + wfsFormaat + "\">\n"); // last attribute
		sb.append(" <wfs:Query \n");
		String typeNames = (version.indexOf("2.0") > -1)?"typeNames":"typeName";
		if (typePrefix==null || typePrefix.isEmpty()){
			sb.append(" " + typeNames + "=\""+typeName+"\"\n");
		} else {
			sb.append(" " + typeNames + "=\"" + typePrefix+":"+typeName+"\"");
			if (typeNameSpace != null && !typeNameSpace.isEmpty())
				sb.append(" xmlns:"+typePrefix+"=\""+typeNameSpace+"\" \n");
		}
		sb.append(" srsName=\""+crs+"\">\n");
		// make wfs filter 
		if (unescapedFilterExpression == null || unescapedFilterExpression.isEmpty()) {
			// no filter
		} else {
				sb.append(" <ogc:Filter>\n");
				sb.append(" <ogc:And>\n");
				sb.append(" " + unescapedFilterExpression + "\n");
				sb.append(" </ogc:And>\n");
				sb.append(" </ogc:Filter>\n");
		}
		sb.append(" </wfs:Query>\n");
		sb.append(" </wfs:GetFeature>\n");
		return sb.toString();
	}

	private String makeGet(String crs, String filterExpression, 
			String typePrefix, String typeNameSpace, String typeName, String version, 
			String wfsFormaat)
			throws UnsupportedEncodingException{
		
		StringBuilder sb = new StringBuilder(uri.toString());
		sb.append("REQUEST=GetFeature&service=WFS&version="+version);

		if (typePrefix==null){
			sb.append("&TYPENAME=" + URLEncoder.encode(typeName, "UTF-8"));
		} else {
			sb.append("&TYPENAME=" + URLEncoder.encode(typePrefix+":"+typeName, "UTF-8"));
		}
		sb.append("&NAMESPACE=xmlns(" + URLEncoder.encode(typeNameSpace, "UTF-8") + ")");

		sb.append("&CRS=" + URLEncoder.encode(crs, "UTF-8"));
		sb.append("&SRSNAME=" + URLEncoder.encode(crs, "UTF-8"));
		// use formaat from fts but replace '+' (=encoded space) with '%20' 
		if (wfsFormaat != null || wfsFormaat.isEmpty()){
			sb.append("&OUTPUTFORMAT="
				+ URLEncoder.encode(wfsFormaat, "UTF-8").replace("+", "%20"));
		}

		// make wfs filter 
		if (filterExpression == null || filterExpression.isEmpty()) {
			// do nothing
		} else {
			sb.append("&filter=" + URLEncoder.encode(filterExpression, "UTF-8").replace("+", "%20"));
		}
		return sb.toString();
	}
	
	/**
	 * Correct a given url string and add a protocol at the start and ? or & at the end.
	 * @param url service url e.g http://host/services/WFS?
	 * @return string corrected url for a WFS/WMS service
	 */
	private String correctServiceUrl (final String url) {
		final String trimmedUrl = (url == null ? "" : url).trim ();
		
		// Add a protocol:
		final String urlWithProtocol;
		if (trimmedUrl.indexOf ("://") < 0) {
			urlWithProtocol = "http://" + trimmedUrl;
		} else {
			urlWithProtocol = trimmedUrl;
		}
		
		// Add '?' or '&':
		if (!urlWithProtocol.endsWith ("?") && !urlWithProtocol.endsWith ("&")) {
			return urlWithProtocol + (urlWithProtocol.contains ("?") ? "&" : "?");
		} else {
			return urlWithProtocol;
		}
	}

}
