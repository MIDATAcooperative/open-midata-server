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

package utils.messaging;

import utils.AccessLog;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.access.EncryptionUtils;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.sync.Instances;

public class ServiceHandler {

	private static byte[] aeskey = null;
	
	public static void setKey(byte[] key) {
		if (aeskey==null) {
			System.out.println("Received background service key");
			aeskey = key;
		}
	}
	
	public static boolean keyAvailable() {
		startup();
		return aeskey != null;
	}
	
	public static byte[] encrypt(String input) throws InternalServerException {
		if (aeskey == null) {
			if (InstanceConfig.getInstance().getInstanceType().disableServiceKeyProtection()) {
				aeskey = new byte[16+4];
			} else {
				utils.sync.Instances.retrieveKey();
				throw new InternalServerException("error.internal", "Background service key missing");
			}
		}
		return EncryptionUtils.randomizeSameAs(EncryptionUtils.encrypt(EncryptionUtils.derandomize(aeskey), input.getBytes()), aeskey);
	}
	
	public static String decrypt(byte[] input) throws InternalServerException {
		if (input == null) return null;
		if (aeskey == null) {
			if (InstanceConfig.getInstance().getInstanceType().disableServiceKeyProtection()) {
				aeskey = new byte[16+4];
			} else {
				utils.sync.Instances.retrieveKey();
				throw new InternalServerException("error.internal", "Background service key missing");			
			}
		}		
		if (!EncryptionUtils.checkMatch(aeskey,  input)) {
			AccessLog.log("Other version of background service key used!");
			return null; 
		}
		return new String(EncryptionUtils.decrypt(EncryptionUtils.derandomize(aeskey), EncryptionUtils.derandomize(input)));
	}
	
	public static void startup() {
		if (aeskey == null) {
			utils.sync.Instances.retrieveKey();
		}
	}
	
	public static void shareKey() {
		if (aeskey != null) {
			System.out.println("Sending background service key");
			Instances.sendKey(aeskey);
		}
	}
}
