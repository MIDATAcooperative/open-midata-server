/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
