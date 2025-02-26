package controllers;

import java.net.MalformedURLException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import com.google.gson.Gson;

import data.MetadataProvider;
import models.DownloadInfo;
import models.DownloadRequest;
import models.DownloadStatus;
import models.MetadataDocument;
import models.OutputFormat;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;
import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;
import play.Configuration;
import play.Logger;
import play.Logger.ALogger;
import play.Routes;
import play.data.Form;
import play.db.Database;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import util.Cache;
import views.html.form;
import views.html.help;
import views.html.feedback;
import views.html.datasetmissing;
import views.html.error;

public class DownloadForm extends Controller {
	
	private final static List<OutputFormat> FORMATS = 
			Collections.unmodifiableList(
				Arrays.asList(
					new OutputFormat("shp", "SHP", "SHAPE-ZIP", "zip"),
					new OutputFormat("csv", "CSV", "csv", "csv"),
					//new OutputFormat("dxf", "DXF", "DXF", "dxf"),
					new OutputFormat("geojson", "GeoJSON", "application/json", "json"),
					//new OutputFormat("geopackage", "Geopackage", "geopackage", "gpkg"),
					new OutputFormat("gml21", "GML 2.1", "text/xml; subtype=gml/2.1.2", "gml"),
					new OutputFormat("gml32", "GML 3.2", "text/xml; subtype=gml/3.2", "gml"),
					new OutputFormat("kml", "KML", "application/vnd.google-earth.kml+xml", "kml")));
	
	private final static Pattern urlPattern = Pattern.compile(".*/(.*?)(\\?.*)?$");
	
	private final WebJarAssets webJarAssets;
	
	private final MetadataProvider metadataProvider;
	
	private final DownloadDao downloadDao;
	
	private final DownloadQueue queueClient;
	
	private final String hostname;
	
	private final String downloadUrlPrefix;
	
	private final Path cache;
	
	private static final ALogger log = Logger.of(DownloadForm.class);
	
	@Inject
	public DownloadForm(WebJarAssets webJarAssets,  MetadataProvider metadataProvider, Database database, Configuration config) {
		this.webJarAssets = webJarAssets;
		this.metadataProvider = metadataProvider;
		this.downloadDao = new DownloadDao(database.getDataSource());
		this.queueClient = new DownloadQueueClient(config.getString("beanstalk.host"), config.getString("beanstalk.queue"));
		
		String hostname = System.getenv("HOSTNAME");
		if(hostname == null) {
			log.warn("HOSTNAME environment variable missing, using 'localhost' instead");
			hostname = "localhost";
		} else {
			log.debug("using HOSTNAME environment variable value: " + hostname);
		}
		
		this.hostname = hostname + ":" + config.getString("play.server.http.port");
		this.downloadUrlPrefix = config.getString("download.url.prefix");
		
		log.debug("generating absolute urls with hostname: " + this.hostname);
		
		cache = Cache.get(config);
	}
	
	/**
	 * Provides a download form for a metadata document.
	 * 
	 * @param id metadata document id
	 * @return http response
	 */
	public Promise<Result> get(String id) {
		return metadataProvider.get(id).map(optionalMetadataDocument -> {
			if(optionalMetadataDocument.isPresent()) {
				MetadataDocument metadataDocument = optionalMetadataDocument.get();
				log.debug("metadataDocument: " + metadataDocument.getTitle());
				return ok(form.render(
					webJarAssets,
					id,
					new DownloadInfo(
						metadataDocument.getTitle(),
						metadataDocument.getBrowseGraphicUrl(),
						metadataDocument.getDescription().length() > 640 ?
								metadataDocument.getDescription().substring(0, 640) + "..." :
								metadataDocument.getDescription(),
						FORMATS),
					Form.form(DownloadRequest.class)));
			} else {
				return notFound(datasetmissing.render(webJarAssets, id));
			}
		});
	}
	
	private static Optional<AdditionalData> createAdditionalData(String url) {
		Matcher urlMatcher = urlPattern.matcher(url);
		if(urlMatcher.matches()) {
			String name = urlMatcher.group(1).trim();
			if(name.isEmpty()) {
				log.warn("url doesn't contain a file name" + url);
				return Optional.empty();
			}
			
			AdditionalData supplementalInformation = new AdditionalData();
			supplementalInformation.setName(urlMatcher.group(1));
			supplementalInformation.setUrl(url);
			return Optional.of(supplementalInformation);
		} else {
			log.warn("url has unfamiliar pattern: " + url);
			return Optional.empty();
		}
	}
	
	/**
	 * Receives a download request forms and creates a download job 
	 * based on the form content and a metadata document. 
	 * 
	 * @param id metadata document id
	 * @return http response
	 */
	public Promise<Result> post(String id) {
		return metadataProvider.get(id).map(optionalMetadataDocument -> {
			if(optionalMetadataDocument.isPresent()) {
				MetadataDocument metadataDocument = optionalMetadataDocument.get();
				
				Form<DownloadRequest> downloadRequestForm =  Form.form(DownloadRequest.class).bindFromRequest();
					
				// check if form is filled in correctly
				if(downloadRequestForm.hasErrors()) {
					return badRequest(form.render(
							webJarAssets,
							id,
							new DownloadInfo(
								metadataDocument.getTitle(), 
								metadataDocument.getBrowseGraphicUrl(), 
								metadataDocument.getDescription(), 
								FORMATS),
							downloadRequestForm));
				}
				
				DownloadRequest downloadRequest = downloadRequestForm.get();
				
				// find choosen output format
				OutputFormat outputFormat = 
					FORMATS.stream()
						.filter(format -> format.name().equals(downloadRequest.getFormat()))
						.findAny()
						.get();

				// specify feature type
				WfsFeatureType ft = new WfsFeatureType();
				ft.setServiceUrl(metadataDocument.getWFSUrl());

				QName featureTypeName = metadataDocument.getFeatureTypeName();
				String namespaceURI = featureTypeName.getNamespaceURI();
				String namespacePrefix = featureTypeName.getPrefix();

				ft.setName(featureTypeName.getLocalPart());
				if(!XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
					ft.setNamespaceUri(namespaceURI);
				}
				if(!XMLConstants.DEFAULT_NS_PREFIX.equals(namespacePrefix)) {
					ft.setNamespacePrefix(namespacePrefix);
				}
				// specify mimetype and extension
				ft.setWfsMimetype(outputFormat.mimeType());
				ft.setExtension(outputFormat.extension());
				//specify crs and version
				ft.setCrs("urn:x-ogc:def:crs:EPSG:28992");
				ft.setServiceVersion("2.0.0");
				
				// add metadata document and stylesheet
				List<AdditionalData> additionalData = new ArrayList<>();
				
				AdditionalData metadata = new AdditionalData();
				metadata.setName("leesmij.xml");
				metadata.setUrl(routes.Metadata.get(id)
					.absoluteURL(false, hostname));
				additionalData.add(metadata);
				
				try {
					metadataDocument.getStylesheet().ifPresent(stylesheetUrl -> {
						log.debug("adding stylesheet: " + stylesheetUrl);

						AdditionalData stylesheet = new AdditionalData();
						stylesheet.setName("metadata.xsl");
						stylesheet.setUrl(stylesheetUrl.toExternalForm());

						Map<String, String> stylesheetHeaders = new HashMap<>();
						stylesheetHeaders.put(metadataProvider.getTrustedHeader(), metadataProvider.getTrustedValue());
						stylesheet.setHeaders(stylesheetHeaders);

						additionalData.add(stylesheet);
					});
				} catch(MalformedURLException e) {
					throw new RuntimeException(e);
				}
				
				// add supplemental information
				for(String url : metadataDocument.getSupplementalInformationUrls()) {
					log.debug("adding supplemental information: " + url);
					createAdditionalData(url).ifPresent(additionalData::add);
				}
				
				// add browse graphic
				String browseGraphicUrl = metadataDocument.getBrowseGraphicUrl();
				log.debug("adding browse graphic: " + browseGraphicUrl);
				createAdditionalData(browseGraphicUrl).ifPresent(additionalData::add);
				
				Download download = new Download();
				download.setName(id);
				download.setFt(ft);
				download.setAdditionalData(additionalData);
				
				// requestId is unique
				String requestId = UUID.randomUUID().toString();  

				// put a download job in the queue
				log.debug("put a download job in the queue");
				Long jobId = null;
				nl.idgis.downloadtool.domain.DownloadRequest downloadReq = new nl.idgis.downloadtool.domain.DownloadRequest(requestId);
				log.debug("processDownloadRequest " + downloadReq);
				downloadReq.setDownload(download);
				downloadReq.setConvertToMimetype(outputFormat.mimeType());
				jobId = queueClient.sendDownloadRequest(downloadReq);

				// store information about this job in the database
				DownloadRequestInfo requestInfo = new DownloadRequestInfo(
						requestId,
						jobId == null ? "" : jobId.toString(),
						outputFormat.mimeType(), 
						download);
				log.debug("store information about this job in the database: " + requestInfo.getRequestId());
				downloadDao.createDownloadRequestInfo(requestInfo);
				
				return redirect(controllers.routes.DownloadForm.lobby(requestId));
			} else {
				return notFound(datasetmissing.render(webJarAssets, id));
			}
		});
	}
	
	public Result lobby(String id) throws SQLException {
		try {
			DownloadRequestInfo info = downloadDao.readDownloadRequestInfo(id);
			
			if(info != null) {
				OutputFormat outputFormat = 
						FORMATS.stream()
							.filter(format -> format.mimeType().equals(info.getUserFormat()))
							.findAny()
							.get();
				
				return ok(feedback.render(
					webJarAssets,
					id,
					info.getDownload().getFt().getName(),
					outputFormat
				));
			}
			
			return notFound(datasetmissing.render(webJarAssets, id));
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			throw sqle;
		}
	}
	
	public Result status(String id) {
		try {
			String zipName = id + ".zip";
			String errorName = id + "_ERROR.txt";
			
			Path zipFile = cache.resolve(zipName);
			Path errorFile = cache.resolve(errorName);
			
			DownloadRequestInfo requestInfo = downloadDao.readDownloadRequestInfo(id);
			DownloadResultInfo resultInfo = downloadDao.readDownloadResultInfo(id);
			DownloadStatus status;
			if(requestInfo == null) {
				status = new DownloadStatus(false, null, null, null, null);
			} else if(resultInfo == null) {
				status = new DownloadStatus(true, false, false, null, null);
			} else if(Files.exists(zipFile)) {
				String url = downloadUrlPrefix + "/" + id;
				status = new DownloadStatus(true, false, true, true, url);
			} else if(Files.exists(errorFile)) {
				status = new DownloadStatus(true, false, true, false, null);
			} else {
				status = new DownloadStatus(true, true, null, null, null);
			}
			
			Gson gson = new Gson();
			return ok(gson.toJson(status)).as("application/json");
		} catch(SQLException sqle) {
			sqle.printStackTrace();
			return internalServerError();
		}
	}
	
	public Result help() {
		return ok(help.render(webJarAssets));
	}
	
	public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
			controllers.routes.javascript.DownloadForm.status()
		)).as ("text/javascript");
	}
}