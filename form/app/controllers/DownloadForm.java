package controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import models.DownloadInfo;
import models.DownloadRequest;
import models.OutputFormat;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.form;

public class DownloadForm extends Controller {
	
	private final WebJarAssets webJarAssets;
	
	@Inject
	public DownloadForm(WebJarAssets webJarAssets) {
		this.webJarAssets = webJarAssets;
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
			info(id), 
			Form.form(DownloadRequest.class)));
	}
	
	public Result post() {
		Form<DownloadRequest> downloadForm = 
			Form.form(DownloadRequest.class).bindFromRequest();
		
		if(downloadForm.hasErrors()) {
			String id = downloadForm.field("id").value();
			
			return badRequest(form.render(
				webJarAssets,
				info(id), 
				downloadForm));
		}
		
		DownloadRequest request = downloadForm.get();
		
		return ok("form ok");
	}
}