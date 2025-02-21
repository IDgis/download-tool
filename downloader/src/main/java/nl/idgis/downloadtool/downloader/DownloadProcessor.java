package nl.idgis.downloadtool.downloader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.idgis.commons.cache.Cache;
import nl.idgis.commons.cache.ZippedCache;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.DownloadResultInfo;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;

/**
 * The DownloadProcessor receives a DownloadRequest bean and performs downloads.<br>
 * 
 * The result of all downloads is packaged into a single zip file.<br>
 * Every single download has its own name and extension.<br>
 * Two types of downloads:<br>
 * 1. from a WFS with optional conversion (GML2SHP, GML2KML, ...)<br>
 * 2. additional data without conversion (metadata.xml, stylesheet, pdf, jpg,
 * ...)<br>
 *
 * @author Rob
 *
 */
public class DownloadProcessor {
	private static final Logger log = LoggerFactory.getLogger(DownloadProcessor.class);

	private static final int BUF_SIZE = 4096;

	private static final String FILENAME_PLACEHOLDER = "X_filename_X";
	
	private DownloadQueue queueClient;

	private final String cachePath;
	private final DownloadDao downloadDao;
	private String genericErrorMessage;
	private String additionalDataFailedFilename = "Download_"+FILENAME_PLACEHOLDER+"_error.txt";

	public DownloadProcessor(String cachePath, DownloadDao downloadDao) {
		super();
		this.cachePath = cachePath;
		this.downloadDao = downloadDao;
		
		log.debug("downloadpath: " + cachePath);
	}

	public void setDownloadQueueClient(DownloadQueue queueClient) {
		this.queueClient = queueClient;
	}

	/**
	 * If an additional download fails, then use this string as the filename in the zip.<br>
	 * @param additionalDataFailedFilename filename of the form "prefixX_filename_Xpostfix". <br>
	 * X_filename_X will be replaced by the original filename.<br>
	 * e.g. "Download_X_filename_X_mislukt" wordt "Download_MetaData_mislukt"
	 */
	private void setAddDataFailedFilename(String additionalDataFailedFilename) {
		this.additionalDataFailedFilename = additionalDataFailedFilename;
	}

	private void setGenericErrorMessage(String genericErrorMessage) {
		this.genericErrorMessage = genericErrorMessage;
	}


	
	/**
	 * Perform downloads using downloadRequest as input.<br>
	 * 1. a cache file is opened.<br>
	 * 2. data from wfs is downloaded into the cache.<br>
	 * 3. additional data is downloaded into the cache.<br>
	 * 4. the cache is closed.<br>
	 * 
	 * @param downloadRequest
	 *            contains all information concerning the downloads requested.
	 */
	@SuppressWarnings("resource")
	public void performDownload(DownloadRequest downloadRequest) throws Exception {
		if (downloadRequest == null) {
			throw new IllegalArgumentException("downloadrequest is null");
		} else {
			Download download = downloadRequest.getDownload();
			if (download == null)
				throw new IllegalArgumentException("downloadrequest does not contain valid downloads");
			
			String fileName = downloadRequest.getRequestId() + ".zip";
			log.debug("creating zip file: {}/{}", cachePath, fileName);

			// requestId is unique and therefore used as name of the zip file
			Cache downloadCache = new ZippedCache(cachePath, fileName);
			// make sure last cache with the same name is deleted before use
			downloadCache.rmCache();
			downloadCache = new ZippedCache(cachePath, fileName);
			OutputStream downloadCacheOutputStream = null;
			try {
				/*
				 * Download Wfs data and put in downloadCache
				 */
				WfsFeatureType ft = download.getFt();
				DownloadSource source = new DownloadWfs(ft);
				downloadCacheOutputStream = downloadData(source, downloadCache, ft.getName() + "." + ft.getExtension());
				/*
				 * Download additional data items and put them in downloadCache
				 */
				List<AdditionalData> additionalData = download.getAdditionalData();
				for (AdditionalData data : additionalData) {
					log.debug("Additional item to downloadCache: " + data.getName());
					source = new DownloadFile(data);
					try {
						downloadCacheOutputStream = downloadData(source, downloadCache, data.getName());
					} catch (Exception ioe) {
						// write exception message into entry in zip
						log.debug("Error: '" + ioe.getMessage() + "' write exception message into entry in zip");
						downloadCacheOutputStream = downloadCache
								.writeItem(additionalDataFailedFilename.replace(FILENAME_PLACEHOLDER, data.getName()));
						InputStream stream;
						if (ioe.getMessage() == null){
							stream = new ByteArrayInputStream(
									genericErrorMessage.getBytes(StandardCharsets.UTF_8));
						} else {
							stream = new ByteArrayInputStream(
									ioe.getMessage().getBytes(StandardCharsets.UTF_8));
						}
						copyStreams(stream, downloadCacheOutputStream);
						stream.close();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// downloadCache.rmCache();
				throw e;
			} finally {
				// close downloadcache stream after all downloads have finished
				if (downloadCacheOutputStream != null)
					downloadCacheOutputStream.close();
				downloadCache.close();
			}
		}
	}

	/**
	 * Download data from a source and copy it into a cache.<br>
	 * Test for a WFS exceptionreport and raise an exception before starting the
	 * copying.
	 * 
	 * @param source
	 *            of the download data (delivers an InputStream which is closed
	 *            in this method)
	 * @param downloadCache
	 *            to write a new item to with content from source
	 * @param fileName
	 *            name of the item in the cache
	 * @param fileExtension
	 *            extension of the item in the cache
	 * @return OutputStream of the cache (which is not closed by this method)
	 * @throws IllegalArgumentException
	 *             if an exceptionreport was received
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private OutputStream downloadData(DownloadSource source, Cache downloadCache, String fileName)
			throws IllegalArgumentException, UnsupportedEncodingException, URISyntaxException, IOException {
			/*
			 * Open source stream
			 */
			BufferedInputStream srcStream = new BufferedInputStream(source.open());
			// test if at http 200 OK an exceptionreport is send instead of the
			// expected content
			testExceptionReport(srcStream);
			/*
			 * Open destination stream
			 */
			OutputStream dstStream = downloadCache.writeItem(fileName);
			/*
			 * Copy from source to destination
			 */
			long byteCount;
			byteCount = copyStreams(srcStream, dstStream);
			log.debug("Data '" + fileName + "' #bytes: " + byteCount);
			srcStream.close();
		return dstStream;
	}

	/**
	 * Process a downloadrequest.<br>
	 * Steps:<br>
	 * <code>
	 * 1. read downloadrequest from queue.<br>
	 * 2. perform actual download(s).<br>
	 * the result will contain the exception message as result code.<br>
	 * 3. remove downloadrequest from queue.<br>
	 * </code>
	 */
	public void processDownloadRequest() {
		DownloadRequest downloadRequest = queueClient.receiveDownloadRequest();
		
		String errorMessage = null;
		try {
			performDownload(downloadRequest);
		} catch (Exception e) {
			e.printStackTrace();
			String msg = e.getMessage();
			errorMessage = msg;
			
			try {
				Path p = Paths.get(getEnv("ZIP_CACHEPATH") + "/" + downloadRequest.getRequestId() + "_ERROR.txt");
				Files.createFile(p);
				Files.write(p, msg.getBytes());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		try {
			DownloadResultInfo info;
			if(errorMessage == null) {
				info = new DownloadResultInfo(downloadRequest.getRequestId(), "OK");
			} else {
				info = new DownloadResultInfo(downloadRequest.getRequestId(), errorMessage);
			}
			this.downloadDao.createDownloadResultInfo(info);
		} catch (SQLException sqle) {
			log.error("Exception when trying to insert result into database");
			sqle.printStackTrace();
		}
		
		try {
			queueClient.deleteDownloadRequest(downloadRequest);
		} catch (Exception e) {
			log.error("Exception when trying to delete message from queue");
			e.printStackTrace();
		}
	}

	private long copyStreams(InputStream is, OutputStream os) throws IOException {
		byte[] b = new byte[8192];
		int read;
		long total = 0;
		while ((read = is.read(b)) != -1) {
			os.write(b, 0, read);
			total += read;
		}
		return total;
	}

	/**
	 * Test if the stream contains an exception report instead of an expected
	 * feature collection.
	 * 
	 * @param fromStream
	 *            stream to test for content "ExceptionReport"
	 * @return true if stream was read and reset, false if the stream has to be
	 *         reopened
	 * @throws IOException
	 *             if stream could not be read
	 * @throws UnsupportedEncodingException
	 * @throws Exception
	 *             containing an ExceptionReport if one occurred
	 */
	private boolean testExceptionReport(BufferedInputStream fromStream)
			throws IOException, UnsupportedEncodingException, IllegalArgumentException {
		int bytesRead = BUF_SIZE - 2;
		boolean markSupported = fromStream.markSupported();
		if (markSupported) {
			fromStream.mark(bytesRead);
			log.debug("Inputstream mark supported, test if ExceptionReport is send and reset stream");
		} else {
			log.debug("Inputstream mark/reset not supported, read stream for ExceptionReport and reopen stream");
		}
		byte[] b = new byte[bytesRead];
		fromStream.read(b);
		String s = new String(b, "UTF-8");
		// remove x00 bytes at the end
		s = s.substring(0, s.lastIndexOf(">") + 1);
		if (s.indexOf("ExceptionReport") > 0) {
			log.debug("found an ExceptionReport: ");
			throw new IllegalArgumentException("ExceptionReport: " + s);
		} else if (s.indexOf("FeatureCollection") > 0) {
			log.trace("found a FeatureCollection: ");
		}
		if (markSupported)
			fromStream.reset();
		return markSupported;
	}
	
	private static String getEnv(String name) {
		String value = System.getenv(name);
		
		if(value == null) {
			throw new IllegalArgumentException(name + " environment variable is missing");
		}
			
		return value;
	}

	public static void main(String... args) {
		String path = getEnv("ZIP_CACHEPATH");		
		String host = getEnv("BEANSTALK_HOST");
		String downloadQueueTubeName = getEnv("BEANSTALK_DOWNLOAD_QUEUE");
		String genericErrorMessage = getEnv("GENERIC_ERROR_MESSAGE");
		String dbUrl = getEnv("DB_URL");
		String dbUser = getEnv("DB_USER");
		String dbPassword = getEnv("DB_PW");
		
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl(dbUrl);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		
		if (genericErrorMessage == null) {
			genericErrorMessage = "Er is een onbekende fout opgetreden bij het downloaden van het huidige bestand.";
		}
		
		String addDataFailedFilename = System.getenv("ADDITIONALDATA_FAILED_FILENAME");
		if (addDataFailedFilename == null){
			addDataFailedFilename = "Download_"+FILENAME_PLACEHOLDER+"_error.txt";
		}
		
		try {
			DownloadDao downloadDao = new DownloadDao(dataSource);
			
			log.info("start loop " + path);
			DownloadProcessor dlp = new DownloadProcessor(path, downloadDao);
			// setup queue clients
			DownloadQueueClient downloadQueueClient = new DownloadQueueClient(host, downloadQueueTubeName);

			dlp.setDownloadQueueClient(downloadQueueClient);
			
			dlp.setGenericErrorMessage(genericErrorMessage);
			
			dlp.setAddDataFailedFilename(addDataFailedFilename);

			for (;;) {
				log.debug("processDownloadRequest");
				dlp.processDownloadRequest();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("end loop ");
		}
	}
}
