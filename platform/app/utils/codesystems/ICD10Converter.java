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

import models.ContentCode;
import utils.exceptions.InternalServerException;

/**
 * Converter for the ICD-10 codesystem. http://hl7.org/fhir/sid/icd-10
 *
 */
public class ICD10Converter implements CodesystemConverter {

	@Override
	public String getCodesystem() {		
		return "http://hl7.org/fhir/sid/icd-10";
	}

	@Override
	public String getContentForCode(String code) throws InternalServerException {
		int p = code.indexOf(".");
		if (p<0) return null;
		String prefix = code.substring(0, p);
		ContentCode cc = ContentCode.getBySystemCode(getCodesystem(), prefix);
		if (cc != null) return cc.content;
		return null;
	}

}
