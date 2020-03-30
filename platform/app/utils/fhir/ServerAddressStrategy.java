package utils.fhir;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import utils.InstanceConfig;

public class ServerAddressStrategy implements IServerAddressStrategy {

	@Override
	public String determineServerBase(ServletContext theServletContext, HttpServletRequest theRequest) {		
		return "https://"+InstanceConfig.getInstance().getPlatformServer()+theRequest.getServletPath();
	}

}
