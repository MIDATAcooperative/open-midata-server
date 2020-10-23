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

package utils.codesystems;

import java.util.HashMap;
import java.util.Map;

import utils.exceptions.InternalServerException;

/**
 * Allows converters codesystem code to midata content type for special codesystems
 *
 */
public class Codesystems {

	private Map<String, CodesystemConverter> converter;
	
	/**
	 * Constructor
	 */
	public Codesystems() {
		converter = new HashMap<String, CodesystemConverter>();
		add(new MidataConverter());
		add(new ICD10Converter());
	}
	
	/**
	 * Add a new codesystem converter
	 * @param conv
	 */
	public void add(CodesystemConverter conv) {
		converter.put(conv.getCodesystem(), conv);
	}
	
	/**
	 * Convert codesystem and code to midata content type
	 * @param system
	 * @param code
	 * @return midata content type or null if code could not be converted
	 * @throws InternalServerException
	 */
	public String getContentForSystemCode(String system, String code) throws InternalServerException {
		CodesystemConverter conv = converter.get(system);
		if (conv != null) return conv.getContentForCode(code);
		return null;
	}
}
