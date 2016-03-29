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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import nl.idgis.downloadtool.domain.AdditionalData;

/**
 * This class represents a file as the source of the download.<br>
 * It is constructed with a url that represents the source and then provides an
 * inputstream from that url.<br>
 * 
 * @author Rob
 * 
 */
public class DownloadFile implements DownloadSource {
	private static final Log log = LogFactory.getLog(DownloadFile.class);
	
	private URI uri;
	private HttpEntity entity;
	private CloseableHttpResponse response; 
	private CloseableHttpClient httpclient;
	private HttpGet httpGet;
	

	/**
	 * Constructor that builds a file download request.
	 * @param additionalData containing all information for the building the request
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException 
	 */
	public DownloadFile(AdditionalData additionalData) throws MalformedURLException,
			UnsupportedEncodingException, URISyntaxException {
		String serviceUrl = additionalData.getUrl();
		uri = new URL(serviceUrl).toURI();
		httpGet = new HttpGet(uri);

		// no authentication foreseen
		String userAuth = null;
		String pwAuth = null;
		httpclient = getHttpClient(userAuth, pwAuth);
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
	
	private CloseableHttpClient getHttpClient(String userAuth, String pwAuth) {
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
}
