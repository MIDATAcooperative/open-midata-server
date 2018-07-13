package utils.servlet;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * HttpServletConfig emulation for Play Framework
 *
 */
public class PlayHttpServletConfig implements ServletConfig {

	private PlayHttpServletContext context = new PlayHttpServletContext();
	
	@Override
	public String getInitParameter(String arg0) {
		throw new RuntimeException("Not implemented");	
		
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		throw new RuntimeException("Not implemented");	
		
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getServletName() {
		return "FHIRServlet";
	}

}
