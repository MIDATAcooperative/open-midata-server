package utils.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import play.mvc.Http;
import scala.NotImplementedError;
import utils.AccessLog;

public class PlayHttpServletRequest implements HttpServletRequest {

	private Http.Request request;
	private Map<String, Object> attribs = new HashMap<String, Object>();
	
	public PlayHttpServletRequest(Http.Request request) {
	   this.request = request;	
	}
	
	@Override
	public AsyncContext getAsyncContext() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Object getAttribute(String arg0) {
		return attribs.get(arg0);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getContentLength() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public long getContentLengthLong() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public String getContentType() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new PlayServletInputStream(new ByteArrayInputStream(request.body().asRaw().asBytes()));
	}

	@Override
	public String getLocalAddr() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getLocalName() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getLocalPort() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public Locale getLocale() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getParameter(String arg0) {
		return request.getQueryString(arg0);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return request.queryString();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String[] getParameterValues(String arg0) {
		return request.queryString().get(arg0);
	}

	@Override
	public String getProtocol() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getRemoteAddr() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getRemoteHost() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getRemotePort() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getScheme() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getServerName() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getServerPort() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public boolean isSecure() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		attribs.remove(arg0);
		
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		attribs.put(arg0,  arg1);				
	}

	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public String changeSessionId() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getAuthType() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public Cookie[] getCookies() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public String getHeader(String arg0) {
		if (arg0.toLowerCase().equals("content-type")) {
			String r = request.getHeader(arg0);
			if (r == null) return "application/json+fhir";
			return r;
		}
				
		return request.getHeader(arg0);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(request.headers().keySet());
	}

	@Override
	public Enumeration<String> getHeaders(String arg0) {
		String[] headers = request.headers().get(arg0);
		if (headers == null) {
			AccessLog.log("header not found:"+arg0);
			if (arg0.toLowerCase().equals("content-type")) return Collections.enumeration(Collections.singleton("application/json+fhir"));
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration(Arrays.asList(headers));
	}

	@Override
	public int getIntHeader(String arg0) {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public String getMethod() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getPathInfo() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getPathTranslated() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getQueryString() {
		String uri = request.uri();
		int i = uri.indexOf('?');
		return i >= 0 ? uri.substring(i+1) : "";
	}

	@Override
	public String getRemoteUser() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getRequestURI() {
		String uri = request.uri();
		int i = uri.indexOf('?');
		return i >= 0 ? uri.substring(0,i) : uri;		
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer result = new StringBuffer("https://demo.midata.coop:9000");
		String uri = request.uri();
		int i = uri.indexOf('?');
		result.append(i >= 0 ? uri.substring(0,i) : uri);
		AccessLog.log(result.toString());
		return result;
	}

	@Override
	public String getRequestedSessionId() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getServletPath() {
		return "/fhir";//request.uri();
	}

	@Override
	public HttpSession getSession() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void logout() throws ServletException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		if (true) throw new NotImplementedError();
		return null;
	}

}
