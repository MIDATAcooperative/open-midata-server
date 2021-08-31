/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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

import akka.util.ByteString;
import play.mvc.Http;
import play.mvc.Http.RawBuffer;
import utils.AccessLog;
import utils.InstanceConfig;

/**
 * HttpServletRequest emulation for Play Framework
 *
 */
public class PlayHttpServletRequest implements HttpServletRequest {

	private Http.Request request;
	private Map<String, Object> attribs = new HashMap<String, Object>();
	private String basePath;
	
	/**
	 * Create HttpServletRequest from Play Http.Request
	 * @param request the play Http.Request
	 */
	public PlayHttpServletRequest(Http.Request request, String basePath) {
	   this.request = request;	
	   this.basePath = basePath;
	}
	
	public PlayHttpServletRequest(Http.Request request) {
		this.request = request;	
		this.basePath = "/fhir";
	}
	
	@Override
	public AsyncContext getAsyncContext() {
		 throw new RuntimeException("Not implemented");
		
	}

	@Override
	public Object getAttribute(String arg0) {
		return attribs.get(arg0);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public String getCharacterEncoding() {
		String ct = request.header("Content-Type").orElseThrow();
		int p = ct.indexOf(";charset=");
		if (p>=0) return ct.substring(p+";charset=".length());
		return "UTF-8";		
	}

	@Override
	public int getContentLength() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public long getContentLengthLong() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String getContentType() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		RawBuffer b = request.body().asRaw();
		long l = b.size();
		ByteString bstr = b.asBytes();
		if (bstr != null) {
		  return new PlayServletInputStream(new ByteArrayInputStream(bstr.toArray()));
		} else {
		  AccessLog.log("ServletInputStream: Read from file");
		  File f = b.asFile();
		  return new PlayServletInputStream(new FileInputStream(f));
		}
	}

	@Override
	public String getLocalAddr() {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public String getLocalName() {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public int getLocalPort() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Locale getLocale() {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new RuntimeException("Not implemented");
		
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
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String[] getParameterValues(String arg0) {
		return request.queryString().get(arg0);
	}

	@Override
	public String getProtocol() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getRealPath(String arg0) {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getRemoteAddr() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getRemoteHost() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public int getRemotePort() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String getScheme() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getServerName() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public int getServerPort() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ServletContext getServletContext() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public boolean isAsyncStarted() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isAsyncSupported() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isSecure() {
		throw new RuntimeException("Not implemented");	
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
		if (true) throw new RuntimeException("Not implemented");
		
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new RuntimeException("Not implemented");		
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String changeSessionId() {
		throw new RuntimeException("Not implemented");		
	}

	@Override
	public String getAuthType() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public Cookie[] getCookies() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public long getDateHeader(String arg0) {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getHeader(String arg0) {
		if (arg0.toLowerCase().equals("content-type")) {
			return request.header(arg0).orElse("application/json+fhir");			
		}
				
		return request.header(arg0).orElse(null);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(request.getHeaders().asMap().keySet());
	}

	@Override
	public Enumeration<String> getHeaders(String arg0) {
		List<String> headers = request.getHeaders().getAll(arg0);
		if (headers == null || headers.isEmpty()) {
			AccessLog.log("header not found:"+arg0);
			if (arg0.toLowerCase().equals("content-type")) return Collections.enumeration(Collections.singleton("application/json+fhir"));
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration(headers);
	}

	@Override
	public int getIntHeader(String arg0) {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getMethod() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new RuntimeException("Not implemented");		
	}

	@Override
	public String getPathInfo() {
		throw new RuntimeException("Not implemented");		
	}

	@Override
	public String getPathTranslated() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getQueryString() {
		String uri = request.uri();
		int i = uri.indexOf('?');
		return i >= 0 ? uri.substring(i+1) : "";
	}

	@Override
	public String getRemoteUser() {
		throw new RuntimeException("Not implemented");		
	}

	@Override
	public String getRequestURI() {
		String uri = request.uri();
		int i = uri.indexOf('?');
		return i >= 0 ? uri.substring(0,i) : uri;		
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer result = new StringBuffer("https://"+InstanceConfig.getInstance().getPlatformServer());
		String uri = request.uri();
		int i = uri.indexOf('?');
		result.append(i >= 0 ? uri.substring(0,i) : uri);
		AccessLog.log(result.toString());
		return result;
	}

	@Override
	public String getRequestedSessionId() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public String getServletPath() {
		return this.basePath;
	}

	@Override
	public HttpSession getSession() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public Principal getUserPrincipal() {
		throw new RuntimeException("Not implemented");		
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isUserInRole(String arg0) {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public void logout() throws ServletException {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		throw new RuntimeException("Not implemented");
	}

}
