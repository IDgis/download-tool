package controllers;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;

import java.net.URL;
import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import data.MetadataProvider;

import models.DownloadInfo;
import models.DownloadRequest;
import models.MetadataDocument;
import models.OutputFormat;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.F.Promise;
import play.db.Database;
import play.Configuration;
import play.Logger;
import play.Logger.ALogger;

import views.html.form;
import views.html.help;
import views.html.feedback;
import views.html.datasetmissing;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;

public class DownloadForm extends Controller {
	
	private final static List<OutputFormat> FORMATS = 
			Collections.unmodifiableList(
				Arrays.asList(
					new OutputFormat("shp", "SHP", "SHAPE-ZIP", "zip"),
					new OutputFormat("csv", "CSV", "csv", "csv"),
					new OutputFormat("dxf", "DXF", "DXF", "dxf"),
					new OutputFormat("geojson", "GeoJSON", "application/json", "json"),
					new OutputFormat("geopackage", "Geopackage", "geopackage", "gpkg"),
					new OutputFormat("gml21", "GML 2.1", "text/xml; subtype=gml/2.1.2", "gml"),
					new OutputFormat("gml32", "GML 3.2", "text/xml; subtype=gml/3.2", "gml"),
					new OutputFormat("kml", "KML", "application/vnd.google-earth.kml+xml", "kml")));
	
	private final static Pattern urlPattern = Pattern.compile(".*/(.*?)(\\?.*)?$");
	
	private final WebJarAssets webJarAssets;
	
	private final MetadataProvider metadataProvider;
	
	private final DownloadDao downloadDao;
	
	private final DownloadQueue queueClient;
	
	private final String hostname;
	
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
		
		log.debug("generating absolute urls with hostname: " + this.hostname);
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

				QName featureTypeName =metadataDocument.getFeatureTypeName();
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
						(jobId==null?"":jobId.toString()), 
						downloadRequest.getName(), downloadRequest.getEmail(), outputFormat.mimeType(), 
						download);
				log.debug("store information about this job in the database: " + requestInfo.getRequestId());
				downloadDao.createDownloadRequestInfo(requestInfo);
				
				
				return ok(feedback.render(
					webJarAssets,
					id,
					metadataDocument.getTitle(),
					outputFormat,
					downloadRequest.getEmail()));
			} else {
				return notFound(datasetmissing.render(webJarAssets, id));
			}
		});
	}
	
	public Result help() {
		return ok(help.render(webJarAssets));
	}
}