package controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

import views.html.form;
import views.html.help;
import views.html.feedback;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.dao.DownloadDao;
import nl.idgis.downloadtool.domain.AdditionalData;

public class DownloadForm extends Controller {
	
	private final static String STYLESHEET = "datasets/intern/metadata.xsl";
	
	private final static List<OutputFormat> FORMATS = 
			Collections.unmodifiableList(
				Arrays.asList(
					new OutputFormat("shp", "SHP"),
					new OutputFormat("gml21", "GML 2.1"),
					new OutputFormat("gml32", "GML 3.2"),
					new OutputFormat("kml", "KML"),
					new OutputFormat("dxf", "DXF")));
	
	private final WebJarAssets webJarAssets;
	
	private final MetadataProvider metadataProvider;
	
	private final DownloadDao downloadDao;
	
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
				
				// TODO: store information about this job in the database
				
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