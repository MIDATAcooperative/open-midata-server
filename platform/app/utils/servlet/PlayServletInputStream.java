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
