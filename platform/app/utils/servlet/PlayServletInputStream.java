package utils.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * ServletInputStream emulation for Play Framework
 *
 */
public class PlayServletInputStream extends ServletInputStream {

	private ByteArrayInputStream in;
	
	/**
	 * Creates a new ServletInputStream from a ByteArrayInputStream
	 * @param in the stream to use
	 */
	public PlayServletInputStream(ByteArrayInputStream in) {
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
