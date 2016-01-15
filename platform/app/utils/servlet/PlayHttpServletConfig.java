package utils.servlet;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import scala.NotImplementedError;

public class PlayHttpServletConfig implements ServletConfig {

	private PlayHttpServletContext context = new PlayHttpServletContext();
	
	@Override
	public String getInitParameter(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		if (true) throw new NotImplementedError();
		return null;
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
