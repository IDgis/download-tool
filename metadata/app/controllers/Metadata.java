package controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.Controller;
import play.libs.F.Promise;

import data.MetadataProvider;

public class Metadata extends Controller {
	
	private final MetadataProvider metadataProvider;
	
	@Inject
	public Metadata(MetadataProvider metadataProvider) {
		this.metadataProvider = metadataProvider;
	}
	
	public Promise<Result> get(String id) {
		return metadataProvider.get(id).map(document -> {
			if(document.isPresent()) {
				return ok(document.get().asBytes()).as("application/xml");
			} else {
				return notFound();
			}
		});
	}
}