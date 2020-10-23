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

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * HttpServletConfig emulation for Play Framework
 *
 */
public class PlayHttpServletConfig implements ServletConfig {

	private PlayHttpServletContext context = new PlayHttpServletContext();
	
	@Override
	public String getInitParameter(String arg0) {
		throw new RuntimeException("Not implemented");	
		
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		throw new RuntimeException("Not implemented");	
		
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getServletName() {
		return "FHIRServlet";
	}

}
