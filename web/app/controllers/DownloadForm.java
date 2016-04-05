package controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

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
import play.Logger;
import play.Logger.ALogger;

import views.html.form;
import views.html.help;
import views.html.feedback;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.queue.DownloadQueue;
import nl.idgis.downloadtool.queue.DownloadQueueClient;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;

public class DownloadForm extends Controller {
	
	private final static String STYLESHEET = "datasets/intern/metadata.xsl";
	
	private final static List<OutputFormat> FORMATS = 
			Collections.unmodifiableList(
				Arrays.asList(
					new OutputFormat("shp", "SHP", "SHAPE-ZIP", "zip"),
					new OutputFormat("gml21", "GML 2.1", "text/xml; subtype=gml/2.1.2", "gml"),
					new OutputFormat("gml32", "GML 3.2", "text/xml; subtype=gml/3.2", "gml"),
					new OutputFormat("kml", "KML", "application/vnd.google-earth.kml+xml", "kml"),
					new OutputFormat("dxf", "DXF", "DXF", "dxf")));
	
	private final WebJarAssets webJarAssets;
	
	private final MetadataProvider metadataProvider;
	
	private final DownloadDao downloadDao;
	
	private DownloadQueue queueClient;
	
	private static final ALogger log = Logger.of(DownloadForm.class);
	
	@Inject
	public DownloadForm(WebJarAssets webJarAssets,  MetadataProvider metadataProvider, Database database) {
		this.webJarAssets = webJarAssets;
		this.metadataProvider = metadataProvider;
		this.downloadDao = new DownloadDao(database.getDataSource());
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
						FORMATS),
					Form.form(DownloadRequest.class)));
			} else {
				return notFound();
			}
		});
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

				ft.setName(featureTypeName.getLocalPart());
				if(!XMLConstants.NULL_NS_URI.equals(featureTypeName.getNamespaceURI())) {
					ft.setNamespaceUri(namespaceURI);
				}
				// specify mimetype and extension
				ft.setWfsMimetype(outputFormat.mimeType());
				ft.setExtension(outputFormat.extension());
				
				// add metadata document and stylesheet
				List<AdditionalData> additionalData = new ArrayList<>();

				AdditionalData stylesheet = new AdditionalData();
				stylesheet.setName("metadata");
				stylesheet.setExtension("xsl");
				stylesheet.setUrl(routes.WebJarAssets.at(webJarAssets.locate(STYLESHEET)).absoluteURL(request()));
				additionalData.add(stylesheet);

				AdditionalData metadata = new AdditionalData();
				metadata.setName(id);
				metadata.setExtension("xml");
				metadata.setUrl(routes.Metadata.get(id).absoluteURL(request()));
				additionalData.add(metadata);

				Download download = new Download();
				download.setName(id);
				download.setFt(ft);
				download.setAdditionalData(additionalData);
				
				// TODO: put a download job in the queue
				log.debug("put a download job in the queue");
				Long jobId = null;
//				nl.idgis.downloadtool.domain.DownloadRequest downloadReq = new nl.idgis.downloadtool.domain.DownloadRequest(id);
//				log.debug("processDownloadRequest " + downloadReq);
//				downloadReq.setDownload(download);
//				downloadReq.setConvertToMimetype(outputFormat.mimeType());
//				jobId = queueClient.sendDownloadRequest(downloadReq);

				// store information about this job in the database
				DownloadRequestInfo requestInfo = new DownloadRequestInfo(
						id, (jobId==null?"":jobId.toString()), 
						downloadRequest.getName(), downloadRequest.getEmail(), outputFormat.mimeType(), 
						download);
				log.debug("store information about this job in the database: " + requestInfo.getRequestId());
				downloadDao.createDownloadRequestInfo(requestInfo);
				
				
				return ok(feedback.render(
					webJarAssets,
					metadataDocument.getTitle(),
					outputFormat,
					downloadRequest.getEmail()));
			} else {
				return notFound();
			}
		});
	}
	
	public Result help() {
		return ok(help.render(webJarAssets));
	}
}