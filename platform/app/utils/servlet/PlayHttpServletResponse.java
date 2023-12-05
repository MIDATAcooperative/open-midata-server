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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import play.http.HttpEntity;
import play.mvc.Result;
import play.mvc.StatusHeader;
import scala.NotImplementedError;
import utils.AccessLog;

/**
 * HttpServletResponse emulation for Play Framework
 *
 */
public class PlayHttpServletResponse implements HttpServletResponse {

	private int status = 200;	
	
	private StringWriter responseWriter;
	private ByteArrayOutputStream responseStream;
	private String contentType;
	private Map<String, String> headers = new HashMap<String, String>();
	
	/**
	 * Creates HttpServletResponse
	 * @param response the Play Http.Response
	 */
	public PlayHttpServletResponse() {
		
	}
	
	@Override
	public void flushBuffer() throws IOException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public int getBufferSize() {
		throw new NotImplementedError();		
	}

	@Override
	public String getCharacterEncoding() {
		throw new NotImplementedError();		
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public Locale getLocale() {
		throw new NotImplementedError();		
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (this.responseStream == null) responseStream = new ByteArrayOutputStream(2000);
		return new PlayServletOutputStream(responseStream);		
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.responseWriter == null) this.responseWriter = new StringWriter();
		return new PrintWriter(this.responseWriter);
	}

	@Override
	public boolean isCommitted() {
		throw new NotImplementedError();		
	}

	@Override
	public void reset() {
		this.responseStream = null;
		this.responseWriter = null;
		headers.clear();
		status = 200;		
	}

	@Override
	public void resetBuffer() {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setBufferSize(int arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setCharacterEncoding(String arg0) {
		AccessLog.log("character-encoding:", arg0);
		//this.characterEncoding = arg0;		
	}

	@Override
	public void setContentLength(int arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setContentLengthLong(long arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setContentType(String arg0) {
		AccessLog.log("content-type:", arg0);
		if (arg0 == null) {
			return;		
		}
		contentType = arg0;
		//response.setContentType(arg0);				
	}

	@Override
	public void setLocale(Locale arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void addCookie(Cookie arg0) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void addHeader(String arg0, String arg1) {
		if (arg0.equals("X-Powered-By")) return;
		headers.put(arg0, arg1);		
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		throw new NotImplementedError();
		
	}

	@Override
	public boolean containsHeader(String arg0) {
		throw new NotImplementedError();
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		throw new NotImplementedError();		
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		throw new NotImplementedError();		
	}

	@Override
	public String encodeURL(String arg0) {
		throw new NotImplementedError();		
	}

	@Override
	public String encodeUrl(String arg0) {
		throw new NotImplementedError();		
	}

	@Override
	public String getHeader(String arg0) {
		return headers.get(arg0);		
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.keySet();		
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		throw new NotImplementedError();		
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void sendError(int arg0) throws IOException {
		throw new NotImplementedError();
		
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		throw new NotImplementedError();
		
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		throw new NotImplementedError();
		
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		 throw new NotImplementedError();
		
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		headers.put(arg0, arg1);		
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		 throw new NotImplementedError();
		
	}

	@Override
	public void setStatus(int arg0) {
		this.status = arg0;		
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		 throw new NotImplementedError();
		
	}
		
	public Result asPlayResult() {
		Result result = null;;
		if (responseWriter != null) result = new StatusHeader(status).sendEntity(HttpEntity.fromString(responseWriter.toString(), "utf-8"));
		else if (responseStream != null) result = new StatusHeader(status).sendBytes(responseStream.toByteArray());
		else result = new StatusHeader(status);
		result = result.as(contentType);
		for (Map.Entry<String, String> header : headers.entrySet()) {
			result = result.withHeader(header.getKey(), header.getValue());
		}
		return result;
	}
	
	
	

}
