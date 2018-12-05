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
