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

package utils;

import java.util.Set;

import models.Consent;
import models.Study;
import models.StudyParticipation;
import models.User;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public abstract class FHIRPatientHolder {

	public abstract void addServiceUrl(User user, Consent consent);
	
	public abstract Set<String> getCodesFromExtension(String url);
	
	public abstract Set<String> getValuesFromExtension(String url);
	
	public abstract void populateIdentifier(AccessContext context, Study study, StudyParticipation sp) throws AppException;
}
