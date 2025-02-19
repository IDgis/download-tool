package util;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import controllers.DownloadResult;

import play.Configuration;
import play.Logger;
import play.Logger.ALogger;

public class Cache {
	
	private static final ALogger log = Logger.of(Cache.class);
	
	static public Path get(Configuration config) {
		String cachePath = config.getString("cache.path");
		
		log.debug("cache.path: " + cachePath);
		
		if(cachePath == null) {
			throw new IllegalArgumentException("cache.path configuration missing");
		}
		
		Path cache = FileSystems.getDefault().getPath(cachePath);
		
		if(!Files.exists(cache)) {
			throw new IllegalArgumentException("configured cache location doesn't exists");
		};
		
		return cache;
	}
}
