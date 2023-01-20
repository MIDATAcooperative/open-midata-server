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

package utils.fhir;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.UriType;

import models.Consent;
import models.MidataId;
import models.Study;
import models.StudyParticipation;
import models.User;
import utils.FHIRPatientHolder;
import utils.InstanceConfig;
import utils.access.Feature_Pseudonymization;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class FhirPatientHolderR4 extends FHIRPatientHolder {

	private Patient thePatient;
	
	public FhirPatientHolderR4(Patient thePatient) {
		this.thePatient = thePatient;
	}
	
	public void addServiceUrl(User user, Consent consent) {
		try {
	      String serviceUrl = InstanceConfig.getInstance().getServiceURL()+"?consent="+consent._id+"&login="+URLEncoder.encode(user.email, "UTF-8");
	      thePatient.addExtension(new Extension("http://midata.coop/extensions/service-url", new UriType(serviceUrl)));
		} catch (UnsupportedEncodingException e) {}
	}
	
	public Set<String> getCodesFromExtension(String url) {		
	    Set<String> result = new HashSet<String>();
		for (Extension ext : thePatient.getExtensionsByUrl(url)) {
			String code = ((Coding) ext.getValue()).getCode();
			if (code != null) result.add(code);
		}
		return result;
	}
	
	public Set<String> getValuesFromExtension(String url) {
	  Set<String> result = new HashSet<String>();
	
	  for (Extension ext : thePatient.getExtensionsByUrl("http://midata.coop/extensions/terms-agreed")) {
		   String value = ext.getValue().primitiveValue();
		   if (value != null) result.add(value);
	  }
	  
	  return result;
	}
	
	public void populateIdentifier(AccessContext context, Study study, StudyParticipation sp) throws AppException {
		if (sp != null) {
			Pair<MidataId, String> pseudo = Feature_Pseudonymization.pseudonymizeUser(context, sp);
			thePatient.addIdentifier().setSystem("http://midata.coop/identifier/participant-name").setValue(pseudo.getRight()).setType(new CodeableConcept(new Coding("http://midata.coop/codesystems/project-code",study.code, study.name)));
			thePatient.addIdentifier().setSystem("http://midata.coop/identifier/participant-id").setValue(pseudo.getLeft().toString()).setType(new CodeableConcept(new Coding("http://midata.coop/codesystems/project-code",study.code, study.name)));
		}
	}
	
}
