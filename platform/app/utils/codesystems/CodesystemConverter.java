package utils.codesystems;

import utils.exceptions.InternalServerException;

/**
 * Convert codes to midata content type for a specific codesystem
 *
 */
public interface CodesystemConverter {

	/**
	 * the codesystem this converter supports
	 * @return
	 */
	public String getCodesystem();
	
	/**
	 * convert code of supported codesystem to midata content type or return null
	 * @param code
	 * @return
	 * @throws InternalServerException
	 */
	public String getContentForCode(String code) throws InternalServerException;
	
}
