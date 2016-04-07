import com.google.inject.AbstractModule;

import controllers.ZooKeeper;

public class DownloadModule extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(ZooKeeper.class).asEagerSingleton();
	}
}