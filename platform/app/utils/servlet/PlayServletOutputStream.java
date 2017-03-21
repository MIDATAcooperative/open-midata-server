package utils.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import scala.NotImplementedError;

/**
 * ServletOuputStream emulation for Play Framework
 *
 */
public class PlayServletOutputStream extends ServletOutputStream {

	private ByteArrayOutputStream out;
	
	/**
	 * Creates a new ServletOutputStream for a given ByteArrayOutputStream
	 * @param out the stream to use
	 */
	public PlayServletOutputStream(ByteArrayOutputStream out) {
		this.out = out;
	}
	
	@Override
	public boolean isReady() {
		throw new NotImplementedError();
	}

	@Override
	public void setWriteListener(WriteListener arg0) {
		throw new NotImplementedError();
		
	}

	@Override
	public void write(int arg0) throws IOException {
		out.write(arg0);		
	}
	
	

}
