package utils.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import scala.NotImplementedError;

public class PlayHttpServletContext implements ServletContext {

	@Override
	public Dynamic addFilter(String arg0, String arg1) {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public Dynamic addFilter(String arg0, Filter arg1) {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public void addListener(String arg0) {
		// TODO Auto-generated method stub
		throw new NotImplementedError();
	}

	@Override
	public <T extends EventListener> void addListener(T arg0) {
		// TODO Auto-generated method stub
		throw new NotImplementedError();
	}

	@Override
	public void addListener(Class<? extends EventListener> arg0) {
		// TODO Auto-generated method stub
		throw new NotImplementedError();
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
		throw new NotImplementedError();
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public void declareRoles(String... arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public Object getAttribute(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public ClassLoader getClassLoader() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public ServletContext getContext(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getEffectiveMajorVersion() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public int getEffectiveMinorVersion() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public FilterRegistration getFilterRegistration(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		if (true) throw new NotImplementedError();
		return null;
	}

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
	public JspConfigDescriptor getJspConfigDescriptor() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public String getMimeType(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getMinorVersion() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public URL getResource(String arg0) throws MalformedURLException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String arg0) {
		
		return null;
	}

	@Override
	public Set<String> getResourcePaths(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getServerInfo() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Servlet getServlet(String arg0) throws ServletException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getServletContextName() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Enumeration<String> getServletNames() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public ServletRegistration getServletRegistration(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Enumeration<Servlet> getServlets() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getVirtualServerName() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public void log(String arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void log(Exception arg0, String arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void log(String arg0, Throwable arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void removeAttribute(String arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public boolean setInitParameter(String arg0, String arg1) {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
		if (true) throw new NotImplementedError();
		
	}

}
