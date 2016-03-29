package nl.idgis.downloadtool.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * This interface represents the source of a download.<br>
 * This source can be a WFS but also a file.<br>
 * An implementation can construct a proper path to the source and then provide an inputstream from that source.<br>
 * 
 * @author Rob
 * 
 */
public interface DownloadSource {
	
	/**
	 * Get the uri to the source.
	 */
	public URI getUri();
	
	/**
	 * Open the source and get the inputstream from this source.
	 * @return inputstream
	 * @throws IOException
	 */
	public InputStream open() throws Exception;
	
	/**
	 * Close the source.
	 * @throws Exception
	 */
	public void close() throws Exception;
}
