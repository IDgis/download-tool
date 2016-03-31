package controllers;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import models.OutputFormat;

import play.mvc.Controller;
import play.mvc.Result;

import views.html.form;

public class Form extends Controller {
	
	private final WebJarAssets webJarAssets;
	
	@Inject
	public Form(WebJarAssets webJarAssets) {
		this.webJarAssets = webJarAssets;
	}
	
	public Result get(String id) {
		String title = id;
		
		List<OutputFormat> formats = Arrays.asList(
			new OutputFormat("shp", "SHP"),
			new OutputFormat("gml21", "GML 2.1"),
			new OutputFormat("gml32", "GML 3.2"),
			new OutputFormat("kml", "KML"),
			new OutputFormat("dxf", "DXF"));
		
		return ok(form.render(webJarAssets, title, formats));
	}
	
	public Result post() {
		return ok("form posted");
	}
}