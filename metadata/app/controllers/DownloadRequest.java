package controllers;

import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import data.MetadataProvider;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Controller;
import play.libs.F.Promise;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.WfsFeatureType;
import nl.idgis.downloadtool.domain.AdditionalData;

public class DownloadRequest extends Controller {
	
	private final static String STYLESHEET = "datasets/intern/metadata.xsl";
	
	private final WebJarAssets webJarAssets;
	
	private final MetadataProvider metadataProvider;
	
	@Inject
	public DownloadRequest(WebJarAssets webJarAssets, MetadataProvider metadataProvider) {
		this.webJarAssets = webJarAssets;
		this.metadataProvider = metadataProvider;
	}
	
	public Promise<Result> get(String id) {
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
			
			return ok(Json.toJson(download));
		});
	}
}