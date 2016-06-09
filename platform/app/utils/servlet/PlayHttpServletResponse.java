package utils.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import play.mvc.Content;
import play.mvc.Http;
import play.mvc.Result;

import scala.NotImplementedError;

public class PlayHttpServletResponse implements HttpServletResponse {

	private int status = 200;
	private String characterEncoding;
	private Http.Response response;
	private StringWriter responseWriter;
	private ByteArrayOutputStream responseStream;
	
	public PlayHttpServletResponse(Http.Response response) {
		this.response = response;
	}
	
	@Override
	public void flushBuffer() throws IOException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public int getBufferSize() {
		if (true) throw new NotImplementedError();
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getContentType() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Locale getLocale() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		responseStream = new ByteArrayOutputStream(2000);
		return new PlayServletOutputStream(responseStream);		
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		this.responseWriter = new StringWriter();
		return new PrintWriter(this.responseWriter);
	}

	@Override
	public boolean isCommitted() {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public void reset() {
		if (true) throw new NotImplementedError();
		
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
		this.characterEncoding = arg0;		
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
		response.setContentType(arg0);				
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
		response.setHeader(arg0, arg1);		
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public boolean containsHeader(String arg0) {
		if (true) throw new NotImplementedError();
		return false;
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String encodeUrl(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public String getHeader(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		if (true) throw new NotImplementedError();
		return null;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void sendError(int arg0) throws IOException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		response.setHeader(arg0, arg1);		
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		if (true) throw new NotImplementedError();
		
	}

	@Override
	public void setStatus(int arg0) {
		this.status = arg0;		
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		if (true) throw new NotImplementedError();
		
	}

	public StringWriter getResponseWriter() {
		return responseWriter;
	}

	public ByteArrayOutputStream getResponseStream() {
		return responseStream;
	}
	
	
	

}
