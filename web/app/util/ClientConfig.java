package util;

import com.typesafe.config.Config;

/**
 * Reads client specific texts from the block named by download.client
 * (e.g. overijssel, utrecht). Not every client defines every key: a missing
 * key results in an empty string, like play.Configuration did before Play 2.7.
 */
public class ClientConfig {

	static public String getString(Config config, String key) {
		Config clientConfig = config.getConfig(config.getString("download.client"));

		return clientConfig.hasPath(key) ? clientConfig.getString(key) : "";
	}
}
