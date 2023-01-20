package utils.fhir_stu3;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.UriType;

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

public class FHIRPatientHolderDSTU3 extends FHIRPatientHolder {

	private Patient thePatient;
	
	public FHIRPatientHolderDSTU3(Patient thePatient) {
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
