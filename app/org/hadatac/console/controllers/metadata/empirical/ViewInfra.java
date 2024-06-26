package org.hadatac.console.controllers.metadata.empirical;

import java.util.List;

import org.hadatac.Constants;
import org.hadatac.console.controllers.Application;
import org.hadatac.entity.pojo.Detector;
import org.hadatac.entity.pojo.Instrument;
import org.hadatac.entity.pojo.Platform;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.views.html.metadata.empirical.*;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Controller;

import javax.inject.Inject;

public class ViewInfra extends Controller {

    @Inject
    Application application;

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result viewPlatform(String dir, String filename, String da_uri, String platform_uri, Http.Request request) {
    	try {
    	    platform_uri = java.net.URLDecoder.decode(platform_uri,"UTF8");
    	} catch (Exception e) {
    	}    	
    	System.out.println("platform URI: [" + platform_uri + "]");
    	Platform platform = Platform.find(platform_uri);
        return ok(viewPlatform.render(dir, filename, da_uri, platform,application.getUserEmail(request)));
    }
    
    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
	public Result postViewPlatform(String dir, String filename, String da_uri, String platform_uri, Http.Request request) {
        return viewPlatform(dir, filename, da_uri, platform_uri, request);
    }

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result viewInstrument(String dir, String filename, String da_uri, String instrument_uri, Http.Request request) {
    	try {
    	    instrument_uri = java.net.URLDecoder.decode(instrument_uri,"UTF8");
    	} catch (Exception e) {
    	}    	
    	Instrument instrument = Instrument.find(instrument_uri);
        return ok(viewInstrument.render(dir, filename, da_uri, instrument,application.getUserEmail(request)));
    }
    
    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
	public Result postViewInstrument(String dir, String filename, String da_uri, String instrument_uri, Http.Request request) {
        return viewInstrument(dir, filename, da_uri, instrument_uri, request);
    }

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result viewDetector(String dir, String filename, String da_uri, String detector_uri, Http.Request request) {
    	try {
    	    detector_uri = java.net.URLDecoder.decode(detector_uri,"UTF8");
    	} catch (Exception e) {
    	}    	
    	System.out.println("Detector URI: [" + detector_uri + "]");
    	Detector detector = Detector.find(detector_uri);
        return ok(viewDetector.render(dir, filename, da_uri, detector, application.getUserEmail(request)));
    }
    
    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
	public Result postViewDetector(String dir, String filename, String da_uri, String detector_uri, Http.Request request) {
        return viewDetector(dir, filename, da_uri, detector_uri, request);
    }

}