package util;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.typesafe.config.Config;

import controllers.DownloadResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cache {

	private static final Logger log = LoggerFactory.getLogger(Cache.class);

	static public Path get(Config config) {
		String cachePath = config.getString("cache.path");

		log.debug("cache.path: " + cachePath);

		Path cache = FileSystems.getDefault().getPath(cachePath);
		
		if(!Files.exists(cache)) {
			throw new IllegalArgumentException("configured cache location doesn't exists");
		};
		
		return cache;
	}
}
