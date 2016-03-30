package controllers;

import play.mvc.Result;
import play.mvc.Controller;

public class Metadata extends Controller {
	
	public Result get(String id) {
		return ok("Hello, world!");
	}
}