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
import models.OutputFormat;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.F.Promise;

import views.html.form;
import views.html.help;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.domain.AdditionalData;

public class DownloadForm extends Controller {
	
	private final static String STYLESHEET = "datasets/intern/metadata.xsl";
	
	private final WebJarAssets webJarAssets;
	
	private final MetadataProvider metadataProvider;
	
	@Inject
	public DownloadForm(WebJarAssets webJarAssets,  MetadataProvider metadataProvider) {
		this.webJarAssets = webJarAssets;
		this.metadataProvider = metadataProvider;
	}
	
	private DownloadInfo info(String id) {
		
		List<OutputFormat> formats = Arrays.asList(
			new OutputFormat("shp", "SHP"),
			new OutputFormat("gml21", "GML 2.1"),
			new OutputFormat("gml32", "GML 3.2"),
			new OutputFormat("kml", "KML"),
			new OutputFormat("dxf", "DXF"));
		
		return new DownloadInfo(id, Collections.unmodifiableList(formats));
	}
	
	public Result get(String id) {
		return ok(form.render(
			webJarAssets,
			id,
			info(id), 
			Form.form(DownloadRequest.class)));
	}
	
	public Promise<Result> post(String id) {
		Form<DownloadRequest> downloadForm = 
			Form.form(DownloadRequest.class).bindFromRequest();
		
		if(downloadForm.hasErrors()) {
			return Promise.pure(
				badRequest(form.render(
					webJarAssets,
					id,
					info(id), 
					downloadForm)));
		}
		
		DownloadRequest request = downloadForm.get();
		
		return metadataProvider.get(id).map(document -> {
			if(!document.isPresent()) {
				return notFound();
			}

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

			WfsFeatureType ft = new WfsFeatureType();
			ft.setServiceUrl(document.get().getWFSUrl());

			QName featureTypeName = document.get().getFeatureTypeName();
			String namespaceURI = featureTypeName.getNamespaceURI();

			ft.setName(featureTypeName.getLocalPart());
			if(!XMLConstants.NULL_NS_URI.equals(featureTypeName.getNamespaceURI())) {
				ft.setNamespaceUri(namespaceURI);
			}

			Download download = new Download();
			download.setName(id);
			download.setFt(ft);
			download.setAdditionalData(additionalData);

			return ok("job created");
		});
	}
	
	public Result help() {
		return ok(help.render(webJarAssets));
	}
}