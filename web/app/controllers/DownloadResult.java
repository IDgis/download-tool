package controllers;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.webjars.play.WebJarsUtil;

import com.typesafe.config.Config;

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

	private final WebJarsUtil webJarsUtil;

	private final Config config;

	@Inject
	public DownloadResult(WebJarsUtil webJarsUtil, Config config, Database database) {
		this.webJarsUtil = webJarsUtil;
		this.config = config;

		cache = Cache.get(config);

		downloadDao = new DownloadDao(database.getDataSource());
	}

	public Result get(String id) {
		String fileName = id + ".zip";

		Path file = cache.resolve(fileName);
		if(Files.exists(file)) {
			return ok(file.toFile(), fileName)
				.as("application/zip")
				.withHeader("Content-Disposition", "attachment; filename=" + fileName);
		} else {
			return notFound(missing.render(webJarsUtil, config));
		}
	}
}
