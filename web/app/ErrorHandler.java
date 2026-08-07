import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;

import com.typesafe.config.Config;

import org.webjars.play.WebJarsUtil;

import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Controller;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import views.html.error;

public class ErrorHandler extends DefaultHttpErrorHandler {

	private final WebJarsUtil webJarsUtil;

	private final Config config;

	@Inject
	public ErrorHandler(WebJarsUtil webJarsUtil, Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
		super(config, environment, sourceMapper, routes);

		this.webJarsUtil = webJarsUtil;
		this.config = config;
	}

	@Override
	protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
		return CompletableFuture.completedFuture(
			Controller.internalServerError(error.render(webJarsUtil, config, exception)));
	}

}
