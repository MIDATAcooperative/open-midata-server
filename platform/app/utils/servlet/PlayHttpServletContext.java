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

/**
 * HttpServletContext emulation for Play Framework
 *
 */
public class PlayHttpServletContext implements ServletContext {

	@Override
	public Dynamic addFilter(String arg0, String arg1) {
		throw new NotImplementedError();		
	}

	@Override
	public Dynamic addFilter(String arg0, Filter arg1) {
		throw new NotImplementedError();
		
	}

	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
		throw new NotImplementedError();
		
	}

	@Override
	public void addListener(String arg0) {		
		throw new NotImplementedError();
	}

	@Override
	public <T extends EventListener> void addListener(T arg0) {		
		throw new NotImplementedError();
	}

	@Override
	public void addListener(Class<? extends EventListener> arg0) {		
		throw new NotImplementedError();
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
		throw new NotImplementedError();		
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
		throw new NotImplementedError();		
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
		throw new NotImplementedError();		
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
		throw new NotImplementedError();		
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
		throw new NotImplementedError();
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
		throw new NotImplementedError();

	}

	@Override
	public void declareRoles(String... arg0) {
		throw new NotImplementedError();

	}

	@Override
	public Object getAttribute(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new NotImplementedError();

	}

	@Override
	public ClassLoader getClassLoader() {
		throw new NotImplementedError();

	}

	@Override
	public ServletContext getContext(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		throw new NotImplementedError();

	}

	@Override
	public int getEffectiveMajorVersion() {
		throw new NotImplementedError();

	}

	@Override
	public int getEffectiveMinorVersion() {
		throw new NotImplementedError();

	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		throw new NotImplementedError();

	}

	@Override
	public FilterRegistration getFilterRegistration(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		throw new NotImplementedError();

	}

	@Override
	public String getInitParameter(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		throw new NotImplementedError();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new NotImplementedError();

	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public String getMimeType(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public int getMinorVersion() {
		throw new NotImplementedError();

	}

	@Override
	public RequestDispatcher getNamedDispatcher(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public String getRealPath(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public URL getResource(String arg0) throws MalformedURLException {
		throw new NotImplementedError();

	}

	@Override
	public InputStream getResourceAsStream(String arg0) {

		return null;
	}

	@Override
	public Set<String> getResourcePaths(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public String getServerInfo() {
		throw new NotImplementedError();

	}

	@Override
	public Servlet getServlet(String arg0) throws ServletException {
		throw new NotImplementedError();

	}

	@Override
	public String getServletContextName() {
		throw new NotImplementedError();

	}

	@Override
	public Enumeration<String> getServletNames() {
		throw new NotImplementedError();

	}

	@Override
	public ServletRegistration getServletRegistration(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		throw new NotImplementedError();

	}

	@Override
	public Enumeration<Servlet> getServlets() {
		throw new NotImplementedError();

	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		throw new NotImplementedError();

	}

	@Override
	public String getVirtualServerName() {
		throw new NotImplementedError();

	}

	@Override
	public void log(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public void log(Exception arg0, String arg1) {
		throw new NotImplementedError();

	}

	@Override
	public void log(String arg0, Throwable arg1) {
		throw new NotImplementedError();

	}

	@Override
	public void removeAttribute(String arg0) {
		throw new NotImplementedError();

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		throw new NotImplementedError();

	}

	@Override
	public boolean setInitParameter(String arg0, String arg1) {
		throw new NotImplementedError();

	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
		throw new NotImplementedError();

	}

}
