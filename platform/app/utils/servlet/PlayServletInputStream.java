package utils.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * ServletInputStream emulation for Play Framework
 *
 */
public class PlayServletInputStream extends ServletInputStream {

	private InputStream in;
	
	/**
	 * Creates a new ServletInputStream from a ByteArrayInputStream
	 * @param in the stream to use
	 */
	public PlayServletInputStream(InputStream in) {
		this.in = in;
	}
	
	@Override
	public boolean isFinished() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public boolean isReady() {
		throw new RuntimeException("Not implemented");	
	}

	@Override
	public void setReadListener(ReadListener arg0) {
		throw new RuntimeException("Not implemented");	
		
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

}
