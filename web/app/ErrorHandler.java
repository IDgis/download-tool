import javax.inject.Inject;
import javax.inject.Provider;

import controllers.WebJarAssets;
import play.Configuration;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import views.html.error;

public class ErrorHandler extends DefaultHttpErrorHandler {
	
	private final WebJarAssets webJarAssets;

	@Inject
	public ErrorHandler(WebJarAssets webJarAssets, Configuration configuration, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
		super(configuration, environment, sourceMapper, routes);
		
		this.webJarAssets = webJarAssets;
	}
	
	@Override
	protected Promise<Result> onProdServerError(RequestHeader request, UsefulException exception) {
		return Promise.pure(Controller.internalServerError(error.render(webJarAssets, exception)));
	}

}
