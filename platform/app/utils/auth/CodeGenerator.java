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

package utils.auth;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;

import org.bson.types.ObjectId;

import models.MidataId;
import models.UsedCode;
import utils.exceptions.InternalServerException;

/**
 * generate public, human readable IDs (like MIDATA ID)
 *
 */
public class CodeGenerator {

	private static SecureRandom random = new SecureRandom();

	/**
	 * Generates a 8 digit code with a "-" after the 4th character
	 * @return the generated code
	 */
	public static String nextCode() {
	    String code = new BigInteger(130, random).toString(32);
	    code = code.toUpperCase();
	    code = code.substring(0,4) + "-" + code.substring(4,8);
	    return code;
	}
	
	public static String nextToken() {
	    String code = new BigInteger(130, random).toString(32);
	    code = code.toUpperCase();
	    code = code.substring(0,6);
	    return code;
	}
	
	/**
	 * Generates a MidataId which does NOT contain the creation time
	 * @return
	 */
	public static MidataId nextMidataId() {
		ObjectId id = new ObjectId(new Date(1000l * (800000000+random.nextInt(700000000))), random.nextInt(16777215));
		return MidataId.from(id);
	}
	
	/**
	 * Generates a 8 digit code with a "-" after the 4th character
	 * Uniqueness is garanteed 
	 * @return the generated code
	 */
	public static String nextUniqueCode() throws InternalServerException {
		String code = nextCode();
		while (!UsedCode.use(code)) { code = nextCode(); }
		return code;
	}
	
	/**
	 * Generate a random passphrase
	 * @return the generated passphrase
	 */
	public static String generatePassphrase() {
		return new BigInteger(130, random).toString(32);
	}
}
