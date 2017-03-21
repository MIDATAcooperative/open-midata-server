package utils.servlet;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import scala.NotImplementedError;

/**
 * HttpServletConfig emulation for Play Framework
 *
 */
public class PlayHttpServletConfig implements ServletConfig {

	private PlayHttpServletContext context = new PlayHttpServletContext();
	
	@Override
	public String getInitParameter(String arg0) {
		throw new NotImplementedError();
		
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		throw new NotImplementedError();
		
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
