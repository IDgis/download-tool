package controllers;

import play.mvc.Result;
import play.mvc.Controller;

public class Download extends Controller {
	
	public Result get(String id) {
		return ok("Hello, world!");
	}
}