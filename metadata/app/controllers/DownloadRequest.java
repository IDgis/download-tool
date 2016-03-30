package controllers;

import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;

import play.libs.Json;
import play.mvc.Result;
import play.mvc.Controller;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.AdditionalData;

public class DownloadRequest extends Controller {
	
	private final static String STYLESHEET = "datasets/intern/metadata.xsl";
	
	private final WebJarAssets webJarAssets;
	
	@Inject
	public DownloadRequest(WebJarAssets webJarAssets) {
		this.webJarAssets = webJarAssets;
	}
	
	public Result get(String id) {
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
		download.setAdditionalData(additionalData);
		
		return ok(Json.toJson(download));
	}
}