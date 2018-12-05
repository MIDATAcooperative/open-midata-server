package utils.codesystems;

import utils.exceptions.InternalServerException;

/**
 * Converter for the midata content type codesystem. http://midata.coop/codesystems/content
 * @author alexander
 *
 */
public class MidataConverter implements CodesystemConverter {

	@Override
	public String getCodesystem() {		
		return "http://midata.coop/codesystems/content";
	}

	@Override
	public String getContentForCode(String code) throws InternalServerException {
		return code;
	}

}
