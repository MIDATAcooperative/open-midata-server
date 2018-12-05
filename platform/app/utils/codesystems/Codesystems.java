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
