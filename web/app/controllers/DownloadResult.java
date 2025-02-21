package controllers;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import play.Configuration;
import play.Logger;
import play.Logger.ALogger;
import play.db.Database;
import play.mvc.Controller;
import play.mvc.Result;
import util.Cache;
import views.html.missing;

import nl.idgis.downloadtool.dao.DownloadDao;

public class DownloadResult extends Controller {
	
	private static final ALogger log = Logger.of(DownloadResult.class);
	
	private final Path cache;
	
	private final DownloadDao downloadDao;
	
	private final WebJarAssets webJarAssets;
	
	@Inject
	public DownloadResult(WebJarAssets webJarAssets, Configuration config, Database database) {
		this.webJarAssets = webJarAssets;
		
		cache = Cache.get(config);
		
		downloadDao = new DownloadDao(database.getDataSource());
	}

	public Result get(String id) {
		String fileName = id + ".zip";
		
		Path file = cache.resolve(fileName);
		if(Files.exists(file)) {
			response().setContentType("application/zip");
			response().setHeader("Content-Disposition", "attachment; filename=" + fileName);
			return ok(file.toFile(), fileName);
		} else {
			return notFound(missing.render(webJarAssets));
		}
	}
}
