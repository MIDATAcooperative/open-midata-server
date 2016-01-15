package utils.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import scala.NotImplementedError;

public class PlayServletInputStream extends ServletInputStream {

	private ByteArrayInputStream in;
	
	public PlayServletInputStream(ByteArrayInputStream in) {
		this.in = in;
	}
	
	@Override
	public boolean isFinished() {
		throw new NotImplementedError();
	}

	@Override
	public boolean isReady() {
		throw new NotImplementedError();
	}

	@Override
	public void setReadListener(ReadListener arg0) {
		throw new NotImplementedError();
		
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

}
